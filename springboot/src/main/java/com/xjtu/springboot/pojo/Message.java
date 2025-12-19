package com.xjtu.springboot.pojo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Message implements Serializable {
    private Long id;
    private Long userId;
    private Long sessionId;
    private Byte role;
    private String thinking;
    private String content;
    private Byte type;
    private String fileIds;
    private Integer tokenCount;
    private Byte isDeepThink;
    private Byte isNetworkSearch;
    private LocalDateTime sendTime;

    @Serial
    private static final long serialVersionUID = 1L;
}