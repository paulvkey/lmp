package com.xjtu.springboot.pojo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserCollection implements Serializable {
    private Long id;
    private Long userId;
    private Long sessionId;
    private String sessionTitle;
    private String collectionNote;
    private LocalDateTime collectedAt;

    private static final long serialVersionUID = 1L;
}