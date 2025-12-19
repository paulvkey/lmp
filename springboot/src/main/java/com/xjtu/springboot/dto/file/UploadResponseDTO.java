package com.xjtu.springboot.dto.file;

import lombok.Data;

import java.util.List;

/**
 * 上传响应DTO
 */
@Data
public class UploadResponseDTO {
    /**
     * 响应码
     */
    private Integer code = 200;

    /**
     * 响应消息
     */
    private String message = "上传成功";

    /**
     * 文件ID（单文件）
     */
    private Long fileId;

    /**
     * 文件ID列表（多文件）
     */
    private List<Long> fileIds;

    /**
     * 已上传分片索引（断点续传）
     */
    private List<Integer> uploadedChunkIndexes;

    /**
     * 文件访问URL
     */
    private String accessUrl;

    /**
     * 文件夹ID（目录上传）
     */
    private Long folderId;

    public static UploadResponseDTO success() {
        return new UploadResponseDTO();
    }

    public static UploadResponseDTO fail(String message) {
        UploadResponseDTO response = new UploadResponseDTO();
        response.setCode(500);
        response.setMessage(message);
        return response;
    }
}
