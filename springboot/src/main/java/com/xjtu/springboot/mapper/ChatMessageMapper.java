package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.ChatMessage;

import java.util.List;

public interface ChatMessageMapper {
    int deleteByPrimaryKey(Long id);

    int insert(ChatMessage row);

    ChatMessage selectByPrimaryKey(Long id);

    List<ChatMessage> selectAll();

    int updateByPrimaryKey(ChatMessage row);

    List<ChatMessage> selectBySessionId(Long sessionId);

    int deleteBySessionId(Long sessionId);
}