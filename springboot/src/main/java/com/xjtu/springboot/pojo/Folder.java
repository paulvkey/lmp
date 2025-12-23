package com.xjtu.springboot.pojo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Folder implements Serializable {
    private Long id;
    private Long userId;
    private Long sessionId;
    private String anonymousId;
    private String name;
    private Long parentId;
    private LocalDateTime createdAt;
    private String uploadId;
    private Byte uploadStatus;
    private Integer totalFiles;
    private Integer uploadedFiles;

    @Serial
    private static final long serialVersionUID = 1L;
}