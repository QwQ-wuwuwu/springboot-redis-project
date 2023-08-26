package com.example.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

@Component
public class JwtConfig {
    @Value("${my.secretKey}")
    private String mySecretKey;
    //加密
    public String encode(Map<String, Object> map) {
        Date now = new Date();
        Date expireyDate = new Date(now.getTime() + 3600 * 24); //设置token过期时间为一天
        return JWT.create()
                .withPayload(map) //设置负载
                .withIssuedAt(now) //设置开始时间
                .withExpiresAt(expireyDate) //设置过期时间
                .sign(Algorithm.HMAC256(mySecretKey)); //设置加密算法，并将密钥添加进去
    }
    //解密
    public DecodedJWT decode(String token) {
        try {
            return JWT.require(Algorithm.HMAC256(mySecretKey))
                    .build().verify(token);
        } catch (TokenExpiredException | JWTDecodeException | SignatureVerificationException e) {
            throw new RuntimeException(e);
        }
    }
}
