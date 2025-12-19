package com.xjtu.springboot.dto.file;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 断点续传校验DTO
 */
@Data
public class BreakpointCheckDTO {
    /**
     * 登录用户ID
     */
    private Long userId;

    /**
     * 匿名用户临时ID
     */
    private String anonymId;

    /**
     * 文件MD5
     */
    @NotBlank(message = "文件MD5不能为空")
    private String fileMd5;

    /**
     * 分片唯一标识
     */
    @NotBlank(message = "uploadId不能为空")
    private String uploadId;
}