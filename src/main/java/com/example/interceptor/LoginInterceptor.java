package com.example.interceptor;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.config.JwtConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtConfig jwtConfig;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("token");
        if (token == null) {
            response.setStatus(401);
            return false;
        }
        //将信息添加到后续的所有请求中，已方便后续使用
        //解密
        DecodedJWT decoded = jwtConfig.decode(token);
        request.setAttribute("userId",decoded.getClaim("userId").asString());
        request.setAttribute("phone",decoded.getClaim("phone").asString());
        return true;
    }
}
