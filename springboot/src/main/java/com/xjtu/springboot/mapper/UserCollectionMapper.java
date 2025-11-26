package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.UserCollection;

import java.util.List;

public interface UserCollectionMapper {
    int deleteByPrimaryKey(Long id);

    int insert(UserCollection row);

    UserCollection selectByPrimaryKey(Long id);

    List<UserCollection> selectAll();

    int updateByPrimaryKey(UserCollection row);

    List<UserCollection> selectAllByUserId(Long userId);

    int deleteBySessionId(Long sessionId);

    UserCollection selectBySessionId(Long sessionId);
}