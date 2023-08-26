package com.example.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Coupon {
    @Id
    private Long id;
    private String Code;
    private String discount;
    private Integer leftNumber; // 剩余数量
    private LocalDateTime expirationTime;
    private String active; // 是否可用
    private String origin;
    private String departure;
    private LocalDateTime startTime;
    private LocalDateTime useTime;
}