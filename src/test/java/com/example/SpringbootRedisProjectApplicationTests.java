package com.example;

import com.example.service.AdminService;
import com.example.service.UserService;
import com.example.util.RedisId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class SpringbootRedisProjectApplicationTests {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Test
    void contextLoads() {
        short s1 = 1;
        s1 = (short) (s1 + 1);
        short s2 = 1;
        s2 += 1;
    }
    @Autowired
    private AdminService adminService;
    @Test
    public void test_01() {
        adminService.saveTicket("1", LocalDateTime.now().plusMinutes(1));
    }
    @Autowired
    private RedisId redisId;
    private ExecutorService service = Executors.newFixedThreadPool(500); // 开启五百个线程
    @Test
    public void test_02() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        Runnable runnable = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisId.nextId("order");
                System.out.println("id:" + id);
            }
            latch.countDown(); // 每提交一次线程执行，计数器减一
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            service.submit(runnable); // 提交三百次
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("costTime:" + (end - begin));
    }
    @Autowired
    private UserService userService;
    @Test
    public void test_03() throws InterruptedException {
        int count = 200;
        CountDownLatch latch = new CountDownLatch(count);
        for(int i = 0; i < count / 2; i++) {
            new Thread(() -> {
                userService.secondsKill(215534614398107751L,"3");
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                latch.countDown();
            }).start();
        }
        for(int i = 0; i < count / 2; i++) {
            new Thread(() -> {
                userService.secondsKill(215534614398107751L,"2");
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                latch.countDown();
            }).start();
        }
        latch.await();
    }
}
