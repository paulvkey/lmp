package com.xjtu.springboot.dto.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.xjtu.springboot.util.DateUtil;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // 新对话：true，历史对话：false
    private Boolean newSession = false;
    private Boolean isLogin = false;
    private Long userId = 0L;
    private Long sessionId = 0L;
    private String sessionTitle;
    private Long modelId = 1L;
    private Byte isPinned = 0;
    private Byte isCollected = 0;
    private Byte isDeleted = 0;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt = DateUtil.now();
    private LocalDateTime lastMsgTime = DateUtil.now();

    // 1：用户，2：AI
    private Byte role;
    private List<MsgDto> messageList;
    private LocalDateTime sendTime = DateUtil.now();
    private Integer tokenCount = 0;
    private Byte isDeepThink = 0;
    private Byte isNetworkSearch = 0;

    public void copyFrom(ChatDto chatDto) {
        this.isLogin = chatDto.getIsLogin();
    	this.userId = chatDto.getUserId();
        this.sessionId = chatDto.getSessionId();
        this.sessionTitle = chatDto.getSessionTitle();
        this.modelId = chatDto.getModelId();
        this.isPinned = chatDto.getIsPinned();
        this.isCollected = chatDto.getIsCollected();
        this.isDeleted = chatDto.getIsDeleted();
        this.createdAt = chatDto.getCreatedAt();
        this.updatedAt = chatDto.getUpdatedAt();
        this.lastMsgTime = chatDto.getLastMsgTime();
        this.role = chatDto.getRole();
        this.messageList = chatDto.getMessageList();
        this.sendTime = chatDto.getSendTime();
        this.tokenCount = chatDto.getTokenCount();
        this.isDeepThink = chatDto.getIsDeepThink();
        this.isNetworkSearch = chatDto.getIsNetworkSearch();
    }
}
