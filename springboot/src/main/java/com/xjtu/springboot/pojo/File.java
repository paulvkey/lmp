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
    private Byte isImage;
    private Integer imageWidth;
    private Integer imageHeight;
    private LocalDateTime uploadAt;
    private String uploadId;
    private Integer uploadedChunks;
    private Byte uploadStatus;
    private String fileMd5;
    private String storageType;
    private String bucketName;
    private LocalDateTime expireAT;
    private String mimeType;
    private Byte isCompressed;
    private String relativePath;

    @Serial
    private static final long serialVersionUID = 1L;

    public void copyFrom(File source) {
        this.setId(source.getId());
        this.setUserId(source.getUserId());
        this.setSessionId(source.getSessionId());
        this.setAnonymousId(source.getAnonymousId());
        this.setFolderId(source.getFolderId());
        this.setNewName(source.getNewName());
        this.setOriginalName(source.getOriginalName());
        this.setExtension(source.getExtension());
        this.setSize(source.getSize());
        this.setStoragePath(source.getStoragePath());
        this.setAccessUrl(source.getAccessUrl());
        this.setIsImage(source.getIsImage());
        this.setImageWidth(source.getImageWidth());
        this.setImageHeight(source.getImageHeight());
        this.setUploadAt(source.getUploadAt());
        this.setUploadId(source.getUploadId());
        this.setUploadedChunks(source.getUploadedChunks());
        this.setUploadStatus(source.getUploadStatus());
        this.setFileMd5(source.getFileMd5());
        this.setStorageType(source.getStorageType());
        this.setBucketName(source.getBucketName());
        this.setExpireAT(source.getExpireAT());
        this.setMimeType(source.getMimeType());
        this.setIsCompressed(source.getIsCompressed());
        this.setRelativePath(source.getRelativePath());
    }
}