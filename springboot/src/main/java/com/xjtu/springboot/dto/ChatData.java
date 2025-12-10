package com.xjtu.springboot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.xjtu.springboot.util.DateUtil;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatData implements Serializable {
    private static final long serialVersionUID = 1L;

    // 新对话：true，历史对话：false
    private Boolean newSession = false;
    private Boolean isLogin = false;
    private Long userId = 0L;
    // 新对话不传递
    private Long sessionId = 0L;
    private String sessionTitle;
    // 暂时固定 1
    private Long aiModelId = 1L;
    private Byte isPinned = 0;
    private Byte isCollected = 0;
    private Byte isDeleted = 0;
    // 新对话不传递
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt = DateUtil.now();
    private LocalDateTime lastMessageTime = DateUtil.now();

    // 1：用户，2：AI
    private Byte messageType;
    private List<Msg> messageList;
    private LocalDateTime sendTime = DateUtil.now();
    private String fileIds;
    private Integer tokenCount = 0;
    private Byte isDeepThink = 0;
    private Byte isNetworkSearch = 0;

    public void copyFrom(ChatData chatData) {
        this.isLogin = chatData.getIsLogin();
    	this.userId = chatData.getUserId();
        this.sessionId = chatData.getSessionId();
        this.sessionTitle = chatData.getSessionTitle();
        this.aiModelId = chatData.getAiModelId();
        this.isPinned = chatData.getIsPinned();
        this.isCollected = chatData.getIsCollected();
        this.isDeleted = chatData.getIsDeleted();
        this.createdAt = chatData.getCreatedAt();
        this.updatedAt = chatData.getUpdatedAt();
        this.lastMessageTime = chatData.getLastMessageTime();
        this.messageType = chatData.getMessageType();
        this.messageList = chatData.getMessageList();
        this.sendTime = chatData.getSendTime();
        this.fileIds = chatData.getFileIds();
        this.tokenCount = chatData.getTokenCount();
        this.isDeepThink = chatData.getIsDeepThink();
        this.isNetworkSearch = chatData.getIsNetworkSearch();
    }
}
