package com.xjtu.springboot.exception;

import lombok.Data;

@Data
public class CustomException extends RuntimeException {
    private final Integer code;
    private final String msg;

    public CustomException(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
