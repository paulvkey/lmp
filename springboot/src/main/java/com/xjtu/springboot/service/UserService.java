package com.xjtu.springboot.service;

import com.xjtu.springboot.dto.UserProfileDto;
import com.xjtu.springboot.exception.CustomException;
import com.xjtu.springboot.mapper.UserProfileMapper;
import com.xjtu.springboot.mapper.UserMapper;
import com.xjtu.springboot.pojo.User;
import com.xjtu.springboot.pojo.UserProfile;
import com.xjtu.springboot.util.DateUtil;
import com.xjtu.springboot.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Objects;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private JwtUtil jwtUtil;

    public UserProfileDto selectUserProfile(UserProfileDto userProfileDto) {
        User res = userMapper.selectByUsername(userProfileDto.getUsername());
        if (Objects.isNull(res)) {
            throw new CustomException(500, "用户不存在");
        }

        return generateUserProfile(res);
    }

    public UserProfileDto generateUserProfile(User res) {
        UserProfile userProfile = userProfileMapper.selectByUserId(res.getId());
        if  (Objects.isNull(userProfile)) {
            throw new CustomException(500, "查询用户信息异常");
        }
        return generateUserProfile(res, userProfile);
    }

    public UserProfileDto generateUserProfile(User user, UserProfile userProfile) {
        UserProfileDto userProfileDto = new UserProfileDto();
        userProfileDto.setUserId(user.getId());
        userProfileDto.setUsername(user.getUsername());
        userProfileDto.setAvatar(userProfile.getAvatar());
        userProfileDto.setSex(userProfile.getSex());
        userProfileDto.setPhone(user.getPhone());
        userProfileDto.setEmail(userProfile.getEmail());
        userProfileDto.setBirthday(userProfile.getBirthday());
        userProfileDto.setBio(userProfile.getBio());
        userProfileDto.setLastLoginIp(userProfile.getLastLoginIp());
        userProfileDto.setLastLoginTime(DateUtil.now());
        userProfileDto.setUpdatedAt(userProfile.getUpdatedAt());
        userProfileDto.setCreatedAt(userProfile.getCreatedAt());

        if (StringUtils.isEmpty(user.getToken()) || !jwtUtil.validateToken(user.getToken(), user)) {
            userProfileDto.setToken(jwtUtil.generateToken(userProfileDto.getUsername(), new HashMap<>()));
        } else {
            userProfileDto.setToken(user.getToken());
        }
        return userProfileDto;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserProfileDto updateInfo(UserProfileDto userProfileDto) {
        User user = userMapper.selectByUsername(userProfileDto.getUsername());
        if (Objects.nonNull(user)) {
            UserProfile userProfile = userProfileMapper.selectByUserId(user.getId());
            if (Objects.nonNull(userProfile)) {
                user.setPhone(userProfileDto.getPhone());
                if (userMapper.updateByPrimaryKey(user) <= 0) {
                    throw new CustomException(500, "更新用户基础信息异常");
                }
                userProfile.setSex(userProfileDto.getSex());
                userProfile.setEmail(userProfileDto.getEmail());
                userProfile.setBirthday(userProfileDto.getBirthday());
                userProfile.setBio(userProfileDto.getBio());
                userProfile.setAvatar(userProfile.getAvatar());
                userProfile.setUpdatedAt(userProfile.getUpdatedAt());
                if (userProfileMapper.updateByPrimaryKey(userProfile) <= 0) {
                    throw new CustomException(500, "更新用户信息异常");
                }
            } else {
                throw new CustomException(500, "未查找到用户信息");
            }

        } else {
            throw new CustomException(500, "未查找到用户");
        }

        return userProfileDto;
    }

    @Transactional(rollbackFor = Exception.class)
    public User updatePwd(String username, String oldPwd, String newPwd) {
        User res = userMapper.selectByUsername(username);
        if (Objects.nonNull(res) && Objects.equals(res.getPassword(), oldPwd)) {
            res.setPassword(newPwd);
            if (userMapper.updatePassword(res) <= 0) {
                throw new CustomException(500, "更新密码异常");
            }
        }

        return res;
    }
}
