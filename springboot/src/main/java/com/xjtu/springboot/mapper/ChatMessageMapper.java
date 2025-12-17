package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageMapper {
    int deleteByPrimaryKey(ChatMessage row);

    int insert(ChatMessage row);

    int updateByPrimaryKey(ChatMessage row);

    int deleteByIds(Long userId, Long sessionId);

    List<ChatMessage> selectByIds(Long userId, Long sessionId);
}