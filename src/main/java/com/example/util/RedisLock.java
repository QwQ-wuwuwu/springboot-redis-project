package com.example.util;

// 基于setnx实现的分布式锁，解决集群模式下的并发问题
public interface RedisLock {
    /*
    * 尝试获取锁，并设置超时时间
    * 成功返回true，失败返回false
    * */
    boolean tryLock(long timeOut);
    // 释放锁
    void unLock();
}
