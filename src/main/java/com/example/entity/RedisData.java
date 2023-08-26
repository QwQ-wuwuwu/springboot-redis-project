package com.example.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedisData {
    // 设置逻辑过期时间
    private LocalDateTime expireTime;
    // 需要保存的数据，如Ticket，完全不需要修改源代码
    private Object data;
}
