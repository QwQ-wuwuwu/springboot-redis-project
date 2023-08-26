package com.example.controller;

import com.example.config.JwtConfig;
import com.example.entity.User;
import com.example.repository.UserRepository;
import com.example.service.UserService;
import com.example.vo.ResultVo;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
public class LoginController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JwtConfig jwtConfig;
    @PostMapping("/code")
    public ResultVo sendCode(@RequestParam("phone") String phone) {
        return userService.isOutTime(phone);
    }
    @PostMapping("/login")
    public ResultVo login(@RequestParam("phone") String phone, @RequestParam("code") String code, HttpServletResponse response) {
        String code1 = redisTemplate.opsForValue().get("user"+phone);
        log.debug("验证码：{}",code1);
        if (phone == null) {
            return ResultVo.error("未填写号码",500);
        }
        if (!code.equals(code1) || code1 == null) {
            return ResultVo.success("验证码错误",500);
        }
        if (userRepository.findByPhone(phone) == null) {
            //对于数据库中没有找到的电话号码，基于电话号码创建该用户
            userService.saveUser(phone);
        }
        //登录成功后，把信息添加到响应头中,注意这里只是把信息以响应的形式返回，并没添加到后续的请求体中
        User user = userRepository.findByPhone(phone);
        Map<String,Object> map = new HashMap<>();
        map.put("userID",user.getId());
        map.put("phone",user.getPhone());
        String token = jwtConfig.encode(map);
        response.addHeader("token",token);
        log.debug("token: {}",token);
        return new ResultVo(map,"登录成功",666);
    }
}
