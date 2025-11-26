package com.xjtu.springboot.dto;

import com.xjtu.springboot.pojo.ChatMessage;
import com.xjtu.springboot.pojo.ChatSession;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SessionData implements Serializable {
    private static final long serialVersionUID = 1L;
    private ChatSession chatSession;
    private List<ChatMessage> chatMessageList;
}
