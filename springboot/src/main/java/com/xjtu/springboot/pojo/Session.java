package com.xjtu.springboot.pojo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Session implements Serializable {
    private Long id;
    private Long userId;
    private Long modelId;
    private String sessionTitle;
    private Byte isPinned;
    private Byte isCollected;
    private Byte isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastMsgTime;

    @Serial
    private static final long serialVersionUID = 1L;
}