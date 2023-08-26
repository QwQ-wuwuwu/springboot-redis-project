package com.example.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResultVo {
    private Map<String, Object> data;
    private String msg;
    private int code;
    public static ResultVo success(String msg, int code) {
        return ResultVo.builder().msg(msg).code(code).build();
    }
    public static ResultVo error(String msg, int code) {
        return ResultVo.builder()
                .code(code)
                .msg(msg).build();
    }
}
