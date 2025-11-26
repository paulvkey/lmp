package com.xjtu.springboot.service;

import com.xjtu.springboot.dto.UserInfoData;
import com.xjtu.springboot.exception.CustomException;
import com.xjtu.springboot.mapper.UserInfoMapper;
import com.xjtu.springboot.mapper.UserMapper;
import com.xjtu.springboot.pojo.User;
import com.xjtu.springboot.pojo.UserInfo;
import com.xjtu.springboot.util.DateUtil;
import com.xjtu.springboot.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private JwtUtil jwtUtil;

    public UserInfoData selectUserInfo(UserInfoData userInfoData) {
        User res = userMapper.selectByUsername(userInfoData.getUsername());
        if (Objects.isNull(res)) {
            throw new CustomException(500, "用户不存在");
        }

        return generateUserInfo(res);
    }

    public UserInfoData generateUserInfo(User res) {
        UserInfo userInfo = userInfoMapper.selectByUserId(res.getId());
        if  (Objects.isNull(userInfo)) {
            throw new CustomException(500, "查询用户信息异常");
        }
        return generateUserInfo(res, userInfo);
    }

    public UserInfoData generateUserInfo(User user, UserInfo userInfo) {
        UserInfoData userInfoData = new UserInfoData();
        userInfoData.setUserId(user.getId());
        userInfoData.setUsername(user.getUsername());
        userInfoData.setPhone(user.getPhone());
        userInfoData.setAvatar(userInfo.getAvatar());
        userInfoData.setEmail(userInfo.getEmail());
        userInfoData.setSex(userInfo.getSex());
        userInfoData.setBirthday(userInfo.getBirthday());
        userInfoData.setStatus(userInfo.getStatus());
        userInfoData.setBio(userInfo.getBio());
        userInfoData.setLastLoginIp(userInfo.getLastLoginIp());
        userInfoData.setLastLoginTime(DateUtil.now());
        userInfoData.setUpdatedAt(userInfo.getUpdatedAt());
        userInfoData.setCreatedAt(userInfo.getCreatedAt());

        if (StringUtils.isEmpty(user.getToken()) || !jwtUtil.validateToken(user.getToken(), user)) {
            userInfoData.setToken(jwtUtil.generateToken(userInfoData.getUsername(), new HashMap<>()));
        } else {
            userInfoData.setToken(user.getToken());
        }
        return userInfoData;
    }

    public List<UserInfoData> selectUserInfos() {
        List<UserInfoData> userInfoDataList = new ArrayList<>();
        List<User> userList = userMapper.selectAll();
        for (User user : userList) {
            userInfoDataList.add(generateUserInfo(user));
        }
        return userInfoDataList;
    }

    @Transactional
    public UserInfoData updateInfo(UserInfoData userInfoData) {
        System.out.println(userInfoData);
        User user = userMapper.selectByUsername(userInfoData.getUsername());
        if (Objects.nonNull(user)) {
            UserInfo userInfo = userInfoMapper.selectByUserId(user.getId());
            if (Objects.nonNull(userInfo)) {
                user.setPhone(userInfoData.getPhone());
                if (userMapper.updateByPrimaryKey(user) <= 0) {
                    throw new CustomException(500, "更新用户基础信息异常");
                }
                userInfo.setEmail(userInfoData.getEmail());
                userInfo.setSex(userInfoData.getSex());
                userInfo.setBirthday(userInfoData.getBirthday());
                userInfo.setStatus(userInfoData.getStatus());
                userInfo.setBio(userInfoData.getBio());
                userInfo.setAvatar(userInfo.getAvatar());
                userInfo.setUpdatedAt(userInfo.getUpdatedAt());
                if (userInfoMapper.updateByPrimaryKey(userInfo) <= 0) {
                    throw new CustomException(500, "更新用户信息异常");
                }
            } else {
                throw new CustomException(500, "未查找到用户信息");
            }

        } else {
            throw new CustomException(500, "未查找到用户");
        }

        return userInfoData;
    }

    @Transactional
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
