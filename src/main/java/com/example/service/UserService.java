package com.example.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Service
@Slf4j
public class LoginSendCode {
    @Autowired
    private StringRedisTemplate redisTemplate;
    public String sendCOde(String phone) {
        Random rand = new Random();
        // 生成随机整数
        int num = rand.nextInt(900000) + 100000;
        // 转成字符串输出
        String randomStr = String.valueOf(num);
        redisTemplate.opsForValue().setIfAbsent("user"+phone,randomStr, Duration.ofDays(300));
        log.debug("验证码：{}",randomStr);
        return randomStr;
    }
}
