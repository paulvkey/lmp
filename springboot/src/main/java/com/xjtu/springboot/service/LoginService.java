package com.xjtu.springboot.service;

import com.xjtu.springboot.dto.UserProfileDto;
import com.xjtu.springboot.exception.CustomException;
import com.xjtu.springboot.mapper.UserProfileMapper;
import com.xjtu.springboot.mapper.UserMapper;
import com.xjtu.springboot.pojo.User;
import com.xjtu.springboot.pojo.UserProfile;
import com.xjtu.springboot.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;


@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserService userService;

    @Transactional(rollbackFor = Exception.class)
    public UserProfileDto login(UserProfileDto userProfileDto) {
        String username = userProfileDto.getUsername();
        String password = userProfileDto.getPassword();
        UserProfileDto res = null;
        if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
            User user = userMapper.selectByUsername(username);
            if (Objects.nonNull(user)) {
                if (!user.getPassword().equals(password)) {
                    throw new CustomException(500, "登录账号或密码错误");
                }
                UserProfile userProfile = userProfileMapper.selectByUserId(user.getId());
                if (Objects.nonNull(userProfile)) {
                    userProfile.setLastLoginTime(DateUtil.now());
                    userProfile.setLastLoginIp(userProfileDto.getLastLoginIp());
                    userProfile.setUpdatedAt(DateUtil.now());
                    if (userProfileMapper.updateByPrimaryKey(userProfile) <= 0) {
                        throw new CustomException(500, "登录信息更新异常");
                    }

                    // 生成返回结果
                    res = userService.generateUserProfile(user, userProfile);
                } else {
                    throw new CustomException(500, "登录信息异常");
                }
            } else {
                throw new CustomException(500, "登录账号不存在");
            }
        }

        return res;
    }

    // 注册
    @Transactional(rollbackFor = Exception.class)
    public boolean register(UserProfileDto userProfileDto) {
        if (StringUtils.isNotEmpty(userProfileDto.getUsername())
                && StringUtils.isNotEmpty(userProfileDto.getPassword())) {
            User res = userMapper.selectByUsername(userProfileDto.getUsername());
            if (Objects.isNull(res)) {
                User user = new User();
                user.setUsername(userProfileDto.getUsername());
                user.setPassword(userProfileDto.getPassword());
                user.setPhone(userProfileDto.getPhone());
                user.setToken(null);
                if (userMapper.insert(user) <= 0) {
                    throw new CustomException(500, "注册用户异常");
                }

                UserProfile userProfile = new UserProfile();
                userProfile.setUserId(user.getId());
                userProfile.setUsername(user.getUsername());
                // TODO
                userProfile.setSex(null);
                userProfile.setEmail(null);
                userProfile.setBirthday(null);
                userProfile.setBio(null);
                userProfile.setCreatedAt(DateUtil.now());
                userProfile.setUpdatedAt(DateUtil.now());
                if (userProfileMapper.insert(userProfile) <= 0) {
                    throw new CustomException(500, "注册用户信息异常");
                }
            } else {
                log.error("注册账号已存在, userId: {}， username: {}", res.getId(), res.getUsername());
                throw new CustomException(500, "注册账号已存在");
            }
        }

        return true;
    }
}
