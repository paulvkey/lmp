package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.UserCollection;

import java.util.List;

public interface UserCollectionMapper {
    int deleteByPrimaryKey(UserCollection row);

    int insert(UserCollection row);

    List<UserCollection> selectAll();

    int updateByPrimaryKey(UserCollection row);

    List<UserCollection> selectAllByUserId(Long userId);

    int deleteByIds(Long userId, Long sessionId);

    UserCollection selectByIds(Long userId, Long sessionId);
}