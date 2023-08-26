package com.example.controller;

import com.example.entity.Ticket;
import com.example.service.UserService;
import com.example.service.UserServiceAsynchronous;
import com.example.vo.ResultVo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {
    @Autowired
    private UserService userService;
    @GetMapping("/tickets")
    public ResultVo getTickets(@RequestParam("origin") String origin, @RequestParam("departure") String departure) {
        List<Ticket> tickets = userService.getTickets(origin,departure);
        if (tickets == null) {
            return ResultVo.builder()
                    .code(404)
                    .msg("没有车次").build();
        }
        return ResultVo.builder()
                .code(666)
                .data(Map.of("tickets",tickets))
                .msg("查找成功").build();
    }
    //查询某车票的详细信息
    @GetMapping("/ticket/{id}")
    public ResultVo getTicket(@PathVariable("id") String id) {
        Ticket ticket = userService.getTicketMutex(id);
        if (ticket == null) {
            return ResultVo.builder().msg("查询失败，无此车次").code(404).build();
        }
        return ResultVo.builder()
                .code(666)
                .msg("查询成功")
                .data(Map.of("ticket",ticket))
                .build();
    }
    @PostMapping("/coupon/{id}")
    public ResultVo secondsKill(@PathVariable("id") Long id/*,@RequestAttribute("userId") String userId*/) {
        String userId = "1";
        return userService.secondsKill(id,userId);
    }
    @Autowired
    private UserServiceAsynchronous asynchronous;
    @PostMapping("/couponAs/{id}")
    public ResultVo secondsKillAs(@PathVariable("id") Long id /*,@RequestAttribute("userId") String userId*/) {
        String userId = "2";
        return asynchronous.secondsKillAs(id,userId);
    }
}
