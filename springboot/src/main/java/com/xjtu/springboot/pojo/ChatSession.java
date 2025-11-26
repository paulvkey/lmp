package com.xjtu.springboot.pojo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatSession implements Serializable {
    private Long id;
    private Long userId;
    private Long aiModelId;
    private String sessionTitle;
    private Byte isPinned;
    private Byte isCollected;
    private Byte isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastMessageTime;

    private static final long serialVersionUID = 1L;
}