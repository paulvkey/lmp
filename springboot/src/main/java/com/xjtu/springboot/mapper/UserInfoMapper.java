package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.UserInfo;

import java.util.List;

public interface UserInfoMapper {
    int deleteByPrimaryKey(Long id);

    int insert(UserInfo row);

    UserInfo selectByPrimaryKey(Long id);

    List<UserInfo> selectAll();

    int updateByPrimaryKey(UserInfo row);

    UserInfo selectByUserId(Long id);
}