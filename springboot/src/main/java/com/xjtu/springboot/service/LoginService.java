package com.xjtu.springboot.service;

import com.xjtu.springboot.dto.UserInfoData;
import com.xjtu.springboot.exception.CustomException;
import com.xjtu.springboot.mapper.UserInfoMapper;
import com.xjtu.springboot.mapper.UserMapper;
import com.xjtu.springboot.pojo.User;
import com.xjtu.springboot.pojo.UserInfo;
import com.xjtu.springboot.util.DateUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;


@Service
public class LoginService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private UserService userService;

    @Transactional
    public UserInfoData login(UserInfoData userInfoData) {
        String username = userInfoData.getUsername();
        String password = userInfoData.getPassword();
        UserInfoData res = null;
        if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
            User user = userMapper.selectByUsername(username);
            if (Objects.nonNull(user)) {
                if (!user.getPassword().equals(password)) {
                    throw new CustomException(500, "登录账号或密码错误");
                }
                UserInfo userInfo = userInfoMapper.selectByUserId(user.getId());
                if (Objects.nonNull(userInfo)) {
                    userInfo.setLastLoginTime(DateUtil.now());
                    userInfo.setLastLoginIp(userInfoData.getLastLoginIp());
                    userInfo.setUpdatedAt(DateUtil.now());
                    if (userInfoMapper.updateByPrimaryKey(userInfo) <= 0) {
                        throw new CustomException(500, "登录信息更新异常");
                    }

                    // 生成返回结果
                    res = userService.generateUserInfo(user, userInfo);
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
    @Transactional
    public boolean register(UserInfoData userInfoData) {
        if (StringUtils.isNotEmpty(userInfoData.getUsername())
                && StringUtils.isNotEmpty(userInfoData.getPassword())) {
            User res = userMapper.selectByUsername(userInfoData.getUsername());
            if (Objects.isNull(res)) {
                User user = new User();
                user.setUsername(userInfoData.getUsername());
                user.setPassword(userInfoData.getPassword());
                user.setPhone(userInfoData.getPhone());
                user.setToken(null);
                if (userMapper.insert(user) <= 0) {
                    throw new CustomException(500, "注册信息异常");
                }

                UserInfo userInfo = userInfoMapper.selectByUserId(user.getId());
                if (Objects.nonNull(userInfo)) {
                    if (userInfoMapper.deleteByPrimaryKey(userInfo.getId()) > 0) {
                        throw new CustomException(500, "注册信息删除异常");
                    }
                }
                userInfo = new UserInfo();
                userInfo.setUserId(user.getId());
                userInfo.setUsername(user.getUsername());
                userInfo.setEmail(null);
                userInfo.setBirthday(null);
                userInfo.setSex(null);
                userInfo.setStatus((byte) 1);
                userInfo.setBio(null);
                userInfo.setPwdFailedCount((byte) 0);
                userInfo.setCreatedAt(DateUtil.now());
                userInfo.setUpdatedAt(DateUtil.now());
                if (userInfoMapper.insert(userInfo) <= 0) {
                    throw new CustomException(500, "注册信息更新异常");
                }
            } else {
                throw new CustomException(500, "注册账号已存在");
            }
        }

        return true;
    }
}
