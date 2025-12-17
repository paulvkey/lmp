package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.ChatSession;

import java.util.List;

public interface ChatSessionMapper {
    int deleteByPrimaryKey(ChatSession row);

    int insert(ChatSession row);

    List<ChatSession> selectAll();

    int updateByPrimaryKey(ChatSession row);

    List<ChatSession> selectSessionByUserId(Long userId);

    ChatSession selectSessionByIds(Long userId, Long sessionId);
}