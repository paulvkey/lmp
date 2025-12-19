package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.Message;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MessageMapper {
    int deleteByPrimaryKey(Message row);

    int insert(Message row);

    int updateByPrimaryKey(Message row);

    int deleteByIds(@Param("userId") Long userId, @Param("sessionId") Long sessionId);

    List<Message> selectByIds(@Param("userId") Long userId, @Param("sessionId") Long sessionId);
}