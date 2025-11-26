package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.User;

import java.util.List;

public interface UserMapper {
    int insert(User row);

    List<User> selectAll();

    User selectByUsername(String username);

    int updatePassword(User res);

    int updateByPrimaryKey(User user);
}