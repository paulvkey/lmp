package com.xjtu.springboot.pojo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Collection implements Serializable {
    private Long id;
    private Long userId;
    private Long sessionId;
    private String sessionTitle;
    private LocalDateTime createdAt;

    @Serial
    private static final long serialVersionUID = 1L;
}