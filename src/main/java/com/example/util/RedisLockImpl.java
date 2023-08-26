package com.example.util;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

@Data
@NoArgsConstructor
public class RedisLockImpl implements RedisLock {
    private StringRedisTemplate redisTemplate;
    private String lockKey;
    public RedisLockImpl(StringRedisTemplate redisTemplate, String lockKey) {
        this.redisTemplate = redisTemplate;
        this.lockKey = lockKey;
    }
    // 保证每个jvm进来的线程id都是唯一，就不存在误删情况
    private static final String UUID_PREFIX = cn.hutool.core.lang.UUID.randomUUID().toString(true) + "-";
    @Override
    public boolean tryLock(long timeOut) {
        // 获取当前线程的信息
        String name = UUID_PREFIX + Thread.currentThread().getId();
        Boolean success = redisTemplate
                .opsForValue()
                .setIfAbsent("lock:" + lockKey, name, timeOut, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success); // 防止拆箱过程中出现空指针异常
    }
    @Override
    public void unLock() {
        String threadId = UUID_PREFIX + Thread.currentThread().getId();
        String lockId = redisTemplate.opsForValue().get("lock:" + lockKey);
        if (lockId.equals(threadId)) { // 判断锁的id和线程jvm的id是否相同，相同则删掉锁
            redisTemplate.delete("lock:" + lockKey);
        }
    }
}
