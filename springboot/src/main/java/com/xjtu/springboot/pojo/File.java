package com.xjtu.springboot.pojo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class File implements Serializable {
    private Long id;
    private Long userId;
    private Long sessionId;
    private String anonymousId;
    private Long folderId;
    private String newName;
    private String originalName;
    private String extension;
    private Long size;
    private String storagePath;
    private String accessUrl;
    private Short isImage;
    private Integer imageWidth;
    private Integer imageHeight;
    private LocalDateTime uploadAt;
    private String fileMd5;
    private String storageType;
    private String bucketName;
    private LocalDateTime expireAt;
    private String relativePath;
    private Short isDeleted;
    private LocalDateTime updatedAt;

    @Serial
    private static final long serialVersionUID = 1L;

}