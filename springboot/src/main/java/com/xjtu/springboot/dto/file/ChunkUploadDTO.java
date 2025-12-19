package com.xjtu.springboot.dto.file;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 分片上传DTO
 */
@Data
public class ChunkUploadDTO {
    /**
     * 登录用户ID（匿名用户传空）
     */
    private Long userId;

    /**
     * 匿名用户临时ID
     */
    private String anonymId;

    /**
     * 关联会话ID
     */
    private Long sessionId;

    /**
     * 所属文件夹ID
     */
    private Long folderId;

    /**
     * 文件相对路径
     */
    private String relativePath;

    /**
     * 存储类型
     */
    private String storageType = "local";

    /**
     * 存储桶名称
     */
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
     * 当前分片索引（从0开始）
     */
    @Min(value = 0, message = "分片索引不能小于0")
    private Integer chunkIndex;

    /**
     * 分片文件
     */
    @NotNull(message = "分片文件不能为空")
    private MultipartFile chunkFile;

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
}