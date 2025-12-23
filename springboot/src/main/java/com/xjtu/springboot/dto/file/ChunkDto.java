package com.xjtu.springboot.dto.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChunkDto implements Serializable {
    private Long userId;
    private String anonymousId;
    private Long sessionId;
    private Long folderId;
    private String relativePath;
    private String storageType = "local";
    private String bucketName;
    /**
     * 分片唯一标识（文件MD5）
     */
    @NotBlank(message = "uploadId不能为空")
    private String uploadId;
    /**
     * 总分片数
     */
    @Min(value = 1, message = "总分片数不能小于1")
    private Integer totalChunks;
    /**
     * 当前分片索引（从1开始）
     */
    @Min(value = 1, message = "分片索引不能小于1")
    private Integer chunkIndex;
    /**
     * 文件MD5（校验用）
     */
    @NotBlank(message = "文件MD5不能为空")
    private String fileMd5;
    /**
     * 原始文件名
     */
    @NotBlank(message = "原始文件名不能为空")
    private String originalFileName;
    /**
     * 文件大小（总大小，字节）
     */
    @Min(value = 1, message = "文件大小不能小于1")
    private Long fileSize;

    @Serial
    private static final long serialVersionUID = 1L;
}