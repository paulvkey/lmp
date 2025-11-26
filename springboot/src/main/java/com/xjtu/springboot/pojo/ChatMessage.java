package com.xjtu.springboot.pojo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatMessage implements Serializable {
    private Long id;
    private Long userId;
    private Long sessionId;
    private Byte messageType;
    private String type;
    private String fileIds;
    private Integer tokenCount;
    private Byte isDeepThink;
    private Byte isNetworkSearch;
    private String deepThinkStep;
    private LocalDateTime sendTime;
    private String messageContent;

    private static final long serialVersionUID = 1L;
}