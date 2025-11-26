package com.xjtu.springboot.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjtu.springboot.common.Result;
import com.xjtu.springboot.dto.PwdData;
import com.xjtu.springboot.dto.UserInfoData;
import com.xjtu.springboot.exception.CustomException;
import com.xjtu.springboot.pojo.File;
import com.xjtu.springboot.pojo.User;
import com.xjtu.springboot.service.FileService;
import com.xjtu.springboot.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@RestController
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private FileService fileService;

    @RequestMapping(method = RequestMethod.POST, path = "user/info")
    public Result selectUser(@RequestBody UserInfoData userInfoData) {
        if (Objects.isNull(userInfoData)
                || StringUtils.isEmpty(userInfoData.getUsername())) {
            throw new CustomException(500, "输入用户信息异常");
        }
        UserInfoData userInfo = userService.selectUserInfo(userInfoData);
        if (Objects.nonNull(userInfo)) {
            return Result.success(userInfo);
        }
        return Result.error("用户不存在");
    }

    @RequestMapping(method = RequestMethod.POST, path = "update/info")
    public Result updateInfo(@RequestBody UserInfoData userInfoData) {
        UserInfoData res = userService.updateInfo(userInfoData);
        if (Objects.nonNull(res)) {
            return Result.success(res);
        }

        return Result.error("修改信息异常");
    }

    @RequestMapping(method = RequestMethod.POST, path = "update/pwd")
    public Result resetPwd(@RequestBody PwdData pwdData) {
        User res = userService.updatePwd(pwdData.getUsername(), pwdData.getOldPwd(), pwdData.getNewPwd());
        if (Objects.nonNull(res)) {
            return Result.success(res);
        }

        return Result.error("更新密码异常");
    }

    @RequestMapping(method = RequestMethod.POST, path = "update/avatar")
    public Result updateAvatar(
            @RequestParam("multipartFile") MultipartFile multipartFile,
            @RequestParam("fileJson") String fileJson) {
        try {
            File file = new ObjectMapper().readValue(
                    fileJson,
                    new TypeReference<File>() {
                    }
            );
            return Result.success(fileService.uploadFile(multipartFile, file));
        } catch (JsonProcessingException e) {
            return Result.error("更新头像异常：" + e.getMessage());
        }
    }
}
