package com.example.service;

import cn.hutool.json.JSONUtil;
import com.example.entity.Coupon;
import com.example.entity.RedisData;
import com.example.entity.Ticket;
import com.example.repository.CouponRepository;
import com.example.repository.TicketRepository;
import com.example.util.RedisId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@Slf4j
public class AdminService {
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;
    //实现数据库和缓存一致更新
    @Transactional
    public boolean update(Ticket ticket) {
        if (ticket == null) {
            return false;
        }
        String origin = ticket.getOrigin();
        String departure = ticket.getDeparture();
        String key = origin + ":" + departure;
        ticketRepository.save(ticket);
        //删除redis缓存
        redisTemplate.delete(key);
        return true;
    }

    // 保存ticket逻辑过期时间，以实现使用该方法解决缓存击穿问题
    public void saveTicket(String id, LocalDateTime expireTime) {
        Ticket ticket = ticketRepository.getTicket(id);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        RedisData redisData = new RedisData();
        redisData.setData(ticket);
        redisData.setExpireTime(expireTime);
        String key = "admin" + id;
        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private RedisId redisId;
    // 添加优惠卷
    @Transactional
    public boolean saveCoupon(Coupon coupon, int left) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expire = now.plusDays(30);
        String origin = coupon.getOrigin();
        String departure = coupon.getDeparture();
        String discount = coupon.getDiscount();

        Coupon coupon1 = new Coupon();
        coupon1.setId(redisId.nextId("coupon"));
        coupon1.setLeftNumber(left);
        coupon1.setStartTime(now);
        coupon1.setExpirationTime(expire);
        coupon1.setOrigin(origin);
        coupon1.setDeparture(departure);

        Random random = new Random();
        String code = String.valueOf(random.nextInt(900000) + 100000);

        coupon1.setCode(code);
        coupon1.setActive("true");
        coupon1.setDiscount(discount);

        couponRepository.save(coupon1);
        // 将优惠卷信息保存到redis缓存中
        redisTemplate.opsForValue().set("coupon:" + coupon1.getId(),coupon1.getLeftNumber().toString());
        return true;
    }
}
