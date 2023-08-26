package com.example.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.config.RedissonConfig;
import com.example.entity.*;
import com.example.repository.CouponOrderRepository;
import com.example.repository.CouponRepository;
import com.example.repository.TicketRepository;
import com.example.repository.UserRepository;
import com.example.util.RedisId;
import com.example.util.RedisLockImpl;
import com.example.vo.ResultVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private UserRepository userRepository;
    public String sendCOde(String phone) {
        Random rand = new Random();
        // 生成随机整数
        int num = rand.nextInt(900000) + 100000;//100000到900000
        // 转成字符串输出
        String randomStr = String.valueOf(num);
        redisTemplate.opsForValue().setIfAbsent("user"+phone,randomStr, Duration.ofSeconds(300));
        log.debug("验证码：{}",randomStr);
        return randomStr;
    }
    //验证码有效时间内不允许再次发送
    public ResultVo isOutTime(String phone) {
        Long ttl = redisTemplate.getExpire("user" + phone);
        log.debug("剩余时间{}",ttl);
        if (ttl <= 300 && ttl >= 0) {
            return ResultVo.builder()
                    .msg("已发送验证码，请" + ttl + "秒后再试")
                    .code(500).build();
        }
        sendCOde(phone);
        return ResultVo.success("发送成功",666);
    }
    @Transactional
    public void saveUser(String phone) {
        User user = new User();
        //生成随机name
        int number = 10;
        String name ="";
        Random random = new Random();
        for (int i = 0; i < number; i++) {
            name += (char)('a' + random.nextInt(26));
        }
        user.setName(name);
        user.setPhone(phone);
        user.setInsertTime(LocalDateTime.now());
        user.setPassword(phone);
        userRepository.save(user);
    }
    @Autowired
    private TicketRepository ticketRepository;
    // 基于redis实现缓存快速查询
    public List<Ticket> getTickets(String origin, String departure) {
        String key = origin + ":" + departure;
        // 从redis缓存里查询
        String value = redisTemplate.opsForValue().get(key);
        // 如果redis里有，直接返回
        if (value != null) {
            List<Ticket> ticketList = new ArrayList<>();
            ticketList = JSONUtil.toList(value, Ticket.class);
            return ticketList;
        }
        // 如果缓存中没有数据，则从数据库查询
        List<Ticket> tickets = ticketRepository.getTickets(origin, departure);
        if (tickets == null) {
            return null;
        }
        // 将数据库查询的数据存入缓存
        redisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(tickets),30, TimeUnit.MINUTES);
        return tickets;
    }

    // 基于redis实现缓存快速查询，同时通过缓存空对象（也并非空对象，而是一个标记）解决缓存穿透
    public Ticket getTicket(String id) {
        String json = redisTemplate.opsForValue().get(id);
        if (json != null) {
            if (json.equals("null")) {
                return null;
            }
            return JSONUtil.toBean(json, Ticket.class);
        }
        Ticket ticket = ticketRepository.getTicket(id);
        if (ticket == null) {
            redisTemplate.opsForValue().set(id,"null",2,TimeUnit.MINUTES);
            return null;
        }
        String key = ticket.getId();
        redisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(ticket),30,TimeUnit.MINUTES);
        return ticket;
    }

    // 基于redis命令setnx实现的互斥锁
    // 加锁
    private Boolean tryLock(String key) {
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(key,"随便",10,TimeUnit.MINUTES);
        return flag;
    }
    // 解锁
    private void unLock(String key) {
        redisTemplate.delete(key);
    }
    // 利用互斥锁解决缓存击穿
    public Ticket getTicketMutex(String id) {
        String json = redisTemplate.opsForValue().get(id);
        if (json != null) {
            if (json.equals("null")) {
                return null;
            }
            return JSONUtil.toBean(json, Ticket.class);
        }
        // 重建redis缓存
        // 获取锁
        String lockKey = "ticket" + id;
        Boolean flag = tryLock(lockKey);
        Ticket ticket = ticketRepository.getTicket(id);
        // 判断是否得到锁
        if (!flag) { // 未得到锁，则休眠一段时间后重试
            try {
                Thread.sleep(50);
                return getTicketMutex(id); // 存在栈溢出风险
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (ticket == null) {
            redisTemplate.opsForValue().set(id,"null",2,TimeUnit.MINUTES);
            return null;
        }
        String key = ticket.getId();
        try {
            Thread.sleep(200); //模拟重建的延时
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        redisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(ticket),30,TimeUnit.MINUTES);
        // 释放锁
        unLock(lockKey);
        return ticket;
    }

    // 利用逻辑过期时间解决缓存击穿
    @Autowired
    private AdminService adminService;
    public Ticket getTicketLogicExpireTime(String id) {
        String json = redisTemplate.opsForValue().get("admin" + id);
        // 未命中直接返回null
        if (json == null) {
            return null;
        }
        // 命中，将json反序列化未RedisData对象，查看过期时间
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        Ticket ticket = JSONUtil.toBean((JSONObject) redisData.getData(), Ticket.class); // 高到低，强转
        LocalDateTime expireTime  = redisData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 未过期，返回信息
            return ticket;
        }
        // 过期，缓存重建
        String lockKey = "ticket" + id;
        // 获取互斥锁
        Boolean flag = tryLock(lockKey);
        // 判断是否获取成功
        if (flag) {
            // 成功，开启独立线程，创建缓存
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    adminService.saveTicket(id, LocalDateTime.now().plusMinutes(1L));
                }
            });
            thread.start();
            unLock(lockKey);
        }
        // 失败，直接返回过期数据
        return ticket;
    }

    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private CouponOrderRepository couponOrderRepository;
    @Autowired
    private RedisId redisId;
    @Resource // 作用和autowire一样
    private RedissonClient redissonClient;
    //private ReentrantLock lock = new ReentrantLock();
    /*
    * 秒杀下单，在高并发情况下存在问题，使用synchronized或者lock悲观锁效率低下，建议使用乐观锁解决超卖问题，并实现一人一单
    * 但是在集群模式下还是会存在并发问题，使用分布锁解决*/
    /*public ResultVo secondsKill(Long id,String userId) {
        Coupon coupon = couponRepository.findById(id).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartTime().isAfter(now)) {
            return ResultVo.error("活动未开始",400);
        }
        if (coupon.getExpirationTime().isBefore(now)) {
            return ResultVo.error("活动已结束",400);
        }
        if (coupon.getLeftNumber() < 1) {
            return ResultVo.error("已被抢完",400);
        }
        synchronized (userId.intern()) {// 保证是string对象的值相同才进行加锁，否则不进行加锁，同时又存在事务失效
            // 必须保证先提交事务，再释放锁
            UserService proxy = (UserService) AopContext.currentProxy();
            return proxy.couponCreate(id,userId); // 解决事务失效
        }
    }*/
    public ResultVo secondsKill(Long id,String userId) {
        Coupon coupon = couponRepository.findById(id).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartTime().isAfter(now)) {
            return ResultVo.error("活动未开始",400);
        }
        if (coupon.getExpirationTime().isBefore(now)) {
            return ResultVo.error("活动已结束",400);
        }
        if (coupon.getLeftNumber() < 1) {
            return ResultVo.error("已被抢完",400);
        }
        /*// 创建锁
        RedisLockImpl redisLock = new RedisLockImpl(redisTemplate,"coupon" + userId);
        // 获取锁  这是自己写的锁，也可以使用redisson提供的各种锁，使程序员更专心于业务逻辑*/
        RLock redisLock = redissonClient.getLock("coupon" + userId); // redisson提供的锁
        boolean flag = redisLock.tryLock(); // 设置十秒的时间自动释放锁，防止系统崩溃没能手动释放锁
        if (!flag) {
            // 获取失败
            return ResultVo.error("不可重复下单",400);
        }
        try {
            UserService proxy = (UserService) AopContext.currentProxy();
            return proxy.couponCreate(id,userId); // 解决事务失效
        } finally {
            redisLock.unlock();
        }
    }
    @Transactional
    public ResultVo couponCreate(Long id,String userId) {
        int flag = couponRepository.updateCoupon(id); // 在数据库层面实现了乐观锁，解决超卖问题其实只需要添加条件剩余数量大于0才可以更新
        if (flag <= 0) {
            return ResultVo.error("更新失败",500);
        }
        CouponOrder couponOrder1 = couponOrderRepository.findById(userId);
        if (couponOrder1 != null) {
            return ResultVo.builder()
                    .msg("已购买过，不可再次购买")
                    .code(404).build();
        }
        CouponOrder couponOrder = new CouponOrder();
        couponOrder.setId(redisId.nextId("couponOrder"));
        couponOrder.setUserId(userId);
        couponOrder.setCouponId(id);
        couponOrder.setCreateTime(LocalDateTime.now());
        couponOrderRepository.save(couponOrder);
        return ResultVo.builder().msg("抢到了！").code(666)
                .data(Map.of("couponOrder",couponOrder)).build();
    }
}
