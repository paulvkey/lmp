package com.xjtu.springboot.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 存储策略接口（本地/OSS/MinIO）
 */
public interface StorageStrategy {
    /**
     * 上传单个文件
     * @param file 上传文件
     * @param storagePath 存储路径
     * @param bucketName 存储桶名称（OSS/MinIO）
     * @return 访问URL
     */
    String uploadFile(MultipartFile file, String storagePath, String bucketName);

    /**
     * 上传分片文件
     * @param chunkFile 分片文件
     * @param chunkPath 分片存储路径
     * @param bucketName 存储桶名称
     * @return 分片存储结果
     */
    boolean uploadChunk(MultipartFile chunkFile, String chunkPath, String bucketName);

    /**
     * 合并分片文件
     * @param uploadId 分片标识
     * @param totalChunks 总分片数
     * @param sourcePath 分片存储目录
     * @param targetPath 目标文件路径
     * @param bucketName 存储桶名称
     * @return 合并后的文件访问URL
     */
    String mergeChunks(String uploadId, Integer totalChunks, String sourcePath, String targetPath, String bucketName);

    /**
     * 获取文件输入流
     * @param storagePath 存储路径
     * @param bucketName 存储桶名称
     * @return 输入流
     */
    InputStream getFileInputStream(String storagePath, String bucketName);

    /**
     * 删除文件
     * @param storagePath 存储路径
     * @param bucketName 存储桶名称
     */
    void deleteFile(String storagePath, String bucketName);

    /**
     * 检查文件是否存在
     * @param storagePath 存储路径
     * @param bucketName 存储桶名称
     * @return 是否存在
     */
    boolean exists(String storagePath, String bucketName);

    // 删除前缀下的所有文件（用于OSS/MinIO分片清理）
    void deletePrefix(String prefix, String bucketName);
}