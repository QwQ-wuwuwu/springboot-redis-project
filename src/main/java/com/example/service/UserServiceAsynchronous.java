package com.example.service;

import cn.hutool.core.bean.BeanUtil;
import com.example.entity.CouponOrder;
import com.example.repository.CouponOrderRepository;
import com.example.repository.CouponRepository;
import com.example.util.RedisId;
import com.example.vo.ResultVo;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
@Slf4j
public class UserServiceAsynchronous {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private CouponOrderRepository couponOrderRepository;
    @Autowired
    private RedisId redisId;

    // 异步秒杀，为了保证redis的原子性，选择使用lua脚本实现。这是基于jvm实现的
    private static final DefaultRedisScript<Long> SECONDSKILL_SCRIPT; // 执行lua脚本配置
    static {
        SECONDSKILL_SCRIPT = new DefaultRedisScript<>();
        SECONDSKILL_SCRIPT.setLocation(new ClassPathResource("secondsKillAs.lua"));
        SECONDSKILL_SCRIPT.setResultType(Long.class);
    }
    private static final ExecutorService secondsKill = Executors.newSingleThreadExecutor(); // 开启一个单线程池
    @PostConstruct // 初始化这个类的时候就执行这个线程
    public void init() {
        secondsKill.submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        // 获取消息队列中的消息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS stream.orders >
                        List<MapRecord<String,Object,Object>> list = redisTemplate.opsForStream().read(
                                Consumer.from("g1","c1"),
                                StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                                StreamOffset.create("stream.orders", ReadOffset.lastConsumed())
                        );
                        if (list == null || list.isEmpty()) { // 获取失败，没有消息，继续下一次循环
                            continue; // 开始下一次循环
                        }
                        // 解析list
                        MapRecord<String,Object,Object> mapRecord = list.get(0);
                        Map<Object,Object> values = mapRecord.getValue();
                        CouponOrder couponOrder = BeanUtil.fillBeanWithMap(values,new CouponOrder(),true);
                        couponOrder.setCreateTime(LocalDateTime.now()); // 因为stream消息阻塞队列里没有存贮createTime
                        // 获取成功，可以下单
                        // 生成订单
                        createCouponOrder(couponOrder);
                        // ACK消息确认
                        redisTemplate.opsForStream().acknowledge("stream.orders","g1",mapRecord.getId());
                    } catch (Exception e) { // 处理异常消息，会进入pending-list
                        log.debug("处理订单异常：",e);
                        while (true) {
                            try {
                                List<MapRecord<String,Object,Object>> list = redisTemplate.opsForStream().read(
                                        Consumer.from("g1","c1"),
                                        StreamReadOptions.empty().count(1),
                                        StreamOffset.create("stream.orders", ReadOffset.from("0"))
                                );
                                if (list == null || list.isEmpty()) { // 获取失败，说明pending-list没有异常信息
                                    break; // 结束
                                }
                                // 解析list
                                MapRecord<String,Object,Object> mapRecord = list.get(0);
                                Map<Object,Object> values = mapRecord.getValue();
                                CouponOrder couponOrder = BeanUtil.fillBeanWithMap(values,new CouponOrder(),true);
                                couponOrder.setCreateTime(LocalDateTime.now());
                                createCouponOrder(couponOrder);
                                // ACK消息确认
                                redisTemplate.opsForStream().acknowledge("stream.orders","g1",mapRecord.getId());
                            }catch (Exception exception) {
                                log.error("处理pending-list异常");
                                try {
                                    Thread.sleep(20);
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        });
    }
    private UserServiceAsynchronous proxy; // 父线程代理
    public ResultVo secondsKillAs(Long id, String userId) {
        long couponOrderId = redisId.nextId("couponOrder");
        // 执行lua脚本
        Long result = redisTemplate.execute(
                SECONDSKILL_SCRIPT,
                Collections.emptyList(),
                id.toString(),userId,String.valueOf(couponOrderId) // 三个个参数
        );
        int r = result.intValue();
        // 判断结果是否为0
        if (r != 0) {
            // 不为0，代表没有购买资格
            return ResultVo.builder()
                    .msg(r == 1 ? "库存不足" : "不可重复下单")
                    .code(404).build();
        }
        // 下单完成，开启独立线程完成阻塞队列里的任务
        proxy = (UserServiceAsynchronous) AopContext.currentProxy();
        return ResultVo.success("下单成功",666);
    }

    // jvm阻塞队列实现的
    // 阻塞队列，大量请求的时可能造成内存不足
    //private final BlockingQueue<CouponOrder> orders = new ArrayBlockingQueue<>(1024 * 1024);
    /*@PostConstruct // 初始化这个类的时候就执行这个线程
    public void init() {
        secondsKill.submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        CouponOrder couponOrder = orders.take(); // 获取阻塞队列中的元素
                        // 生成订单
                        createCouponOrder(couponOrder);
                    } catch (Exception e) {
                        log.debug("处理订单异常：",e);
                    }
                }
            }
        });
    }*/
    /*public ResultVo secondsKillAs(Long id, String userId) {
        // 执行lua脚本
        Long result = redisTemplate.execute(
                SECONDSKILL_SCRIPT,
                Collections.emptyList(),
                id.toString(),userId // 两个参数
        );
        int r = result.intValue();
        // 判断结果是否为0
        if (r != 0) {
            // 不为0，代表没有购买资格
            return ResultVo.builder()
                    .msg(r == 1 ? "库存不足" : "不可重复下单")
                    .code(404).build();
        }
        // 为0，将下单信息保存到消息阻塞队列
        long couponOrderId = redisId.nextId("couponOrder");
        CouponOrder couponOrder = new CouponOrder();
        couponOrder.setId(couponOrderId);
        couponOrder.setUserId(userId);
        couponOrder.setCouponId(id);
        couponOrder.setCreateTime(LocalDateTime.now());
        // 将订单信息保存到阻塞队列等待执行
        orders.add(couponOrder);
        // 下单完成，开启独立线程完成阻塞队列里的任务
        proxy = (UserServiceAsynchronous) AopContext.currentProxy();
        return ResultVo.success("下单成功",666);
    }*/
    @Autowired
    private RedissonClient redissonClient;
    public void createCouponOrder(CouponOrder couponOrder) {
        String userId = couponOrder.getUserId();
        RLock redisLock = redissonClient.getLock("lock:coupon:" + userId); // redisson提供的锁
        boolean flag = redisLock.tryLock();
        if (!flag) {
            // 获取失败
            log.error("创建订单失败");
        }
        try {
            proxy.couponCreate(couponOrder); // 解决事务失效
        } finally {
            redisLock.unlock();
        }
    }
    @Autowired
    private CouponRepository couponRepository;
    @Transactional
    public void couponCreate(CouponOrder couponOrder) {
        long id = couponOrder.getCouponId();
        int result = couponRepository.updateCoupon(id);
        if (result <= 0) {
            log.error("更新失败");
        }
        couponOrderRepository.save(couponOrder);
        log.debug("抢单成功");
    }
}
