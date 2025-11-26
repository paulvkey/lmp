package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.ChatSession;

import java.util.List;

public interface ChatSessionMapper {
    int deleteByPrimaryKey(Long id);

    int insert(ChatSession row);

    ChatSession selectByPrimaryKey(Long id);

    List<ChatSession> selectAll();

    int updateByPrimaryKey(ChatSession row);

    List<ChatSession> selectSessionByUserId(Long userId);
}