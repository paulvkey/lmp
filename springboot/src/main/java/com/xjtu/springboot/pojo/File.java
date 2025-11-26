package com.xjtu.springboot.pojo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class File implements Serializable {
    private Long id;
    private Long userId;
    private Long sessionId;
    private Long folderId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private Byte isImage;
    private Integer imageWidth;
    private Integer imageHeight;
    private LocalDateTime uploadTime;

    private static final long serialVersionUID = 1L;

    public void copyFrom(File source) {
        this.setId(source.getId());
        this.setUserId(source.getUserId());
        this.setSessionId(source.getSessionId());
        this.setFolderId(source.getFolderId());
        this.setFileName(source.getFileName());
        this.setFilePath(source.getFilePath());
        this.setFileSize(source.getFileSize());
        this.setIsImage(source.getIsImage());
        this.setImageWidth(source.getImageWidth());
        this.setImageHeight(source.getImageHeight());
        this.setUploadTime(source.getUploadTime());
    }
}