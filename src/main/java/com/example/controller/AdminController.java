package com.example.controller;

import com.example.entity.Coupon;
import com.example.entity.Ticket;
import com.example.service.AdminService;
import com.example.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AdminController {
    @Autowired
    private AdminService adminService;
    @PostMapping("/ticket")
    public ResultVo updateTicket(@RequestBody Ticket ticket) {
        boolean flag = adminService.update(ticket);
        if (!flag) {
            return ResultVo.builder().msg("更新失败").code(401).build();
        }
        return ResultVo.success("更新成功",666);
    }
    @PostMapping("/coupon")
    public ResultVo saveCoupon(@RequestBody Coupon coupon, @RequestParam("left") int left) {
        boolean flag = adminService.saveCoupon(coupon,left);
        if (!flag) {
            return ResultVo.error("保存失败",500);
        }
        return ResultVo.success("保存成功",666);
    }
}
