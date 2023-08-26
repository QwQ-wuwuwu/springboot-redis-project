package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true) // 暴露当前代理对象
@SpringBootApplication
public class SpringbootRedisProjectApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringbootRedisProjectApplication.class, args);
    }

}
