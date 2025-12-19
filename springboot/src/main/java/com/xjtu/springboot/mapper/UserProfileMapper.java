package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.UserProfile;

import java.util.List;

public interface UserProfileMapper {
    int deleteByUserId(Long userId);

    int insert(UserProfile row);

    UserProfile selectByPrimaryKey(Long id);

    int updateByPrimaryKey(UserProfile row);

    UserProfile selectByUserId(Long userId);
}