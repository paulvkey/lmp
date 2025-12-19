package com.xjtu.springboot.controller;

import com.xjtu.springboot.common.Result;
import com.xjtu.springboot.dto.UserProfileDto;
import com.xjtu.springboot.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class LoginController {
    private final LoginService loginService;

    @RequestMapping(method = RequestMethod.POST, path = "/login")
    public Result login(@RequestBody UserProfileDto userProfileDto) {
        UserProfileDto res = loginService.login(userProfileDto);
        if (Objects.nonNull(res)) {
            return Result.success(res);
        } else {
            return Result.error("登录异常，请重试");
        }
    }

    @RequestMapping(method = RequestMethod.POST, path = "/register")
    public Result register(@RequestBody UserProfileDto userProfileDto) {
        if (loginService.register(userProfileDto)) {
            return Result.success();
        } else {
            return Result.error("注册异常，请重试");
        }
    }
}
