package com.xjtu.springboot.exception;

import com.xjtu.springboot.common.Result;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.PrintWriter;
import java.io.StringWriter;

@ControllerAdvice(basePackages = {
        "com.xjtu.springboot.controller",
        "com.xjtu.springboot.service"
})
public class GlobalExceptionHandler {

    // 统一处理所有控制器所有的异常
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result handleException(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        pw.close();
        return Result.error(exception.getMessage());
    }

    @ExceptionHandler(CustomException.class)
    @ResponseBody
    public Result handleException(CustomException exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        pw.close();
        return Result.error(exception.getCode(), exception.getMsg());
    }
}
