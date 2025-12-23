package com.xjtu.springboot.dto.file;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 文件夹上传初始化DTO
 */
@Data
public class FolderInitDTO {
    /**
     * 登录用户ID
     */
    private Long userId;

    /**
     * 匿名用户临时ID
     */
    private String anonymousId;

    /**
     * 关联会话ID
     */
    private Long sessionId;

    /**
     * 文件夹上传唯一标识
     */
    @NotBlank(message = "folderUploadId不能为空")
    private String folderUploadId;

    /**
     * 根文件夹名称
     */
    @NotBlank(message = "文件夹名称不能为空")
    private String folderName;

    /**
     * 文件夹内总文件数
     */
    @Min(value = 1, message = "总文件数不能小于1")
    private Integer totalFiles;
}
