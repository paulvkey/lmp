package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.Session;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SessionMapper {
    int deleteByPrimaryKey(Session row);

    int insert(Session row);

    int updateByPrimaryKey(Session row);

    List<Session> selectSessionByUserId(Long userId);

    Session selectSessionByIds(@Param("userId") Long userId, @Param("sessionId") Long sessionId);
}