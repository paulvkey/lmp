package com.xjtu.springboot.service.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.UUID;

/**
 * 阿里云OSS存储实现
 */
@Slf4j
@Component("ossStorage")
public class OSSStorage implements StorageStrategy {

    @Value("${file.upload.oss.endpoint}")
    private String endpoint;

    @Value("${file.upload.oss.access-key-id}")
    private String accessKeyId;

    @Value("${file.upload.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${file.upload.oss.default-bucket}")
    private String defaultBucket;

    @Override
    public String uploadFile(MultipartFile file, String storagePath, String bucketName) {
        String bucket = bucketName == null ? defaultBucket : bucketName;
        try (OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret)) {
            // 上传文件
            ossClient.putObject(new PutObjectRequest(bucket, storagePath, file.getInputStream()));
            // 生成访问URL（公网）
            return "https://" + bucket + "." + endpoint.replace("http://", "") + "/" + storagePath;
        } catch (IOException e) {
            log.error("OSS文件上传失败", e);
            return null;
        }
    }

    @Override
    public boolean uploadChunk(MultipartFile chunkFile, String chunkPath, String bucketName) {
        String bucket = bucketName == null ? defaultBucket : bucketName;
        try (OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret)) {
            ossClient.putObject(new PutObjectRequest(bucket, chunkPath, chunkFile.getInputStream()));
            return true;
        } catch (IOException e) {
            log.error("OSS分片上传失败", e);
            return false;
        }
    }

    @Override
    public String mergeChunks(String uploadId, Integer totalChunks, String sourcePath, String targetPath, String bucketName) {
        // OSS分片合并需使用MultipartUpload，此处简化实现（下载分片到本地合并后再上传）
        String bucket = bucketName == null ? defaultBucket : bucketName;
        try (OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret)) {
            // 本地临时文件
            File tempFile = new File(System.getProperty("java.io.tmpdir") + File.separator + UUID.randomUUID());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                // 下载所有分片并合并
                for (int i = 0; i < totalChunks; i++) {
                    String chunkPath = sourcePath + "/" + i;
                    OSSObject ossObject = ossClient.getObject(bucket, chunkPath);
                    Files.copy(ossObject.getObjectContent(), fos);
                    // 删除分片
                    ossClient.deleteObject(bucket, chunkPath);
                }
            }
            // 上传合并后的文件
            ossClient.putObject(new PutObjectRequest(bucket, targetPath, new FileInputStream(tempFile)));
            // 删除临时文件
            tempFile.delete();
            // 生成访问URL
            return "https://" + bucket + "." + endpoint.replace("http://", "") + "/" + targetPath;
        } catch (Exception e) {
            log.error("OSS分片合并失败", e);
            return null;
        }
    }

    @Override
    public InputStream getFileInputStream(String storagePath, String bucketName) {
        String bucket = bucketName == null ? defaultBucket : bucketName;
        try (OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret)) {
            OSSObject ossObject = ossClient.getObject(bucket, storagePath);
            return ossObject.getObjectContent();
        } catch (Exception e) {
            log.error("获取OSS文件输入流失败", e);
            return null;
        }
    }

    @Override
    public void deleteFile(String storagePath, String bucketName) {
        String bucket = bucketName == null ? defaultBucket : bucketName;
        try (OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret)) {
            ossClient.deleteObject(bucket, storagePath);
        } catch (Exception e) {
            log.error("删除OSS文件失败", e);
        }
    }

    @Override
    public boolean exists(String storagePath, String bucketName) {
        String bucket = bucketName == null ? defaultBucket : bucketName;
        try (OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret)) {
            return ossClient.doesObjectExist(bucket, storagePath);
        } catch (Exception e) {
            log.error("检查OSS文件是否存在失败", e);
            return false;
        }
    }

    @Override
    public void deletePrefix(String prefix, String bucketName) {
        String bucket = bucketName == null ? defaultBucket : bucketName;
        try (OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret)) {
            // 列举前缀下的所有文件并删除
            ListObjectsV2Request request = new ListObjectsV2Request(bucket);
            request.setPrefix(prefix);
            ListObjectsV2Result result;
            do {
                result = ossClient.listObjectsV2(request);
                for (OSSObjectSummary summary : result.getObjectSummaries()) {
                    ossClient.deleteObject(bucket, summary.getKey());
                }
                request.setContinuationToken(result.getNextContinuationToken());
            } while (result.isTruncated());
        } catch (Exception e) {
            log.error("OSS删除前缀文件失败：prefix={}", prefix, e);
        }
    }
}