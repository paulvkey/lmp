package com.xjtu.springboot.pojo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class FileChunk implements Serializable {
    private Long id;
    private String uploadId;
    private Long fileId;
    private Long userId;
    private Long folderId;
    private String fileMd5;
    private String originalName;
    private Integer totalChunks;
    private Integer uploadedChunks;
    private Integer uploadStatus;
    private String storageType;
    private String bucketName;
    private String relativePath;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    private LocalDateTime expireAt;
    private Byte isDeleted;

    @Serial
    private static final long serialVersionUID = 1L;

}