package com.xjtu.springboot.pojo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class FileFolder implements Serializable {
    private Long id;
    private Long userId;
    private Long sessionId;
    private String folderName;
    private Long parentFolderId;
    private LocalDateTime createdAt;

    private static final long serialVersionUID = 1L;
}