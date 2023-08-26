package com.example.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component // 基于redis id自增长生成全局唯一id，类似于雪花算法
public class RedisId {
    private StringRedisTemplate redisTemplate;
    public RedisId(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    // 开始时间戳
    private static final long beginTime = 1640995200L;
    public long nextId(String key) {
        // 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSeconds = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp = nowSeconds - beginTime;
        // 生成序列号
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count = redisTemplate.opsForValue().increment("icr:" + key + ":" + date);
        // 拼接返回
        return timeStamp << 32 | count;
    }
}
