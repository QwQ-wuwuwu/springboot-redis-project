### 功能介绍
基于redis，springboot，springdata-jpa实现的售票系统。同步，异步，消息队列，redis互斥锁，集群部署下nginx负载均衡
####
实现短信登录，通过jwt对token进行加密，基于redis缓存快速查询车票信息，同时解决了缓存击穿，缓存穿透等问题；
更新车票，同时将更新信息同步到redis缓存；保存优惠卷，同时保存到redis缓存，便于后续异步秒杀；
实现了优惠卷秒杀功能，每个优惠卷基于redis id自增长生成全局唯一id，类似于雪花算法；
同时保证一个人只能下单一次，对于其存在的并发问题使用redis的各种特性解决；
### 出现的问题及解决方案
1. 缓存穿透：大量同一请求反复访问，服务器就会反复创建缓存，导致缓存过多，服务器宕机
2. 缓存击穿：缓存失效，解决方法当然是重新创建缓存，但是在此过程中存在一定延迟，如果多个请求同时访问，大量请求就会直接到达数据库，查询速度大大降低，故使用redis中setnx的互斥性自定义创建互斥锁，重建缓存时去上锁
3. 缓存雪崩：大量redis缓存同时失效
4. 超卖问题：
5. 同步秒杀并发问题：大量请求线程同时到达服务，造成线程混乱，从而导致一张优惠卷被多次下单购买，故使用了悲观锁控制并发，但是弊端就是导致服务器处理请求效率低下
6. 同步秒杀集群部署下的并发问题：synchronized或者lock悲观锁不仅效率低下，而且集群部署情况下悲观锁不能保证每个请求获得同一把锁，造成并发问题，所以引出了redis分布式锁
7. 同步秒杀分布式锁误删问题：
8. 同步秒杀分布式锁的原子性问题：前期使用lua脚本解决，后期使用redisson分布式锁解决
9. redisson可重入锁，看门狗策略：
10. redisson分布式锁，主从特性：
### 改进update
1. 异步秒杀：大大提高运行效率，但是是基于jvm和lua脚本实现的阻塞队列，存在内存不足的情况
2. 异步秒杀进一步优化：基于redis中stream实现消息阻塞队列，解决内存不足
