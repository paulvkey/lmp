package com.xjtu.springboot.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjtu.springboot.common.Result;
import com.xjtu.springboot.dto.PwdDto;
import com.xjtu.springboot.dto.UserProfileDto;
import com.xjtu.springboot.exception.CustomException;
import com.xjtu.springboot.pojo.File;
import com.xjtu.springboot.pojo.User;
import com.xjtu.springboot.service.FileService;
import com.xjtu.springboot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final FileService fileService;

    @RequestMapping(method = RequestMethod.POST, path = "user/info")
    public Result selectUser(@RequestBody UserProfileDto userProfileDto) {
        if (Objects.isNull(userProfileDto)
                || StringUtils.isEmpty(userProfileDto.getUsername())) {
            throw new CustomException(500, "输入用户信息异常");
        }
        UserProfileDto userProfile = userService.selectUserProfile(userProfileDto);
        if (Objects.nonNull(userProfile)) {
            return Result.success(userProfile);
        }
        return Result.error("用户不存在");
    }

    @RequestMapping(method = RequestMethod.POST, path = "update/info")
    public Result updateInfo(@RequestBody UserProfileDto userProfileDto) {
        UserProfileDto res = userService.updateInfo(userProfileDto);
        if (Objects.nonNull(res)) {
            return Result.success(res);
        }

        return Result.error("修改信息异常");
    }

    @RequestMapping(method = RequestMethod.POST, path = "update/pwd")
    public Result resetPwd(@RequestBody PwdDto pwdDto) {
        User res = userService.updatePwd(pwdDto.getUsername(), pwdDto.getOldPwd(), pwdDto.getNewPwd());
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
            // TODO
            return Result.success(fileService.uploadFile(multipartFile, file));
        } catch (JsonProcessingException e) {
            return Result.error("头像信息解析失败：" + e.getMessage());
        } catch (CustomException e) {
            return Result.error(e.getCode(), e.getMsg());
        }
    }
}
