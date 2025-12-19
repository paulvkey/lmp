package com.xjtu.springboot.service.storage;

import com.xjtu.springboot.dto.file.UploadResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地存储实现
 */
@Slf4j
@Component("localStorage")
public class LocalStorage implements StorageStrategy {

    /**
     * 本地存储根目录（配置在application.yml）
     */
    @Value("${file.upload.local.root-path:/data/upload}")
    private String localRootPath;

    /**
     * 本地访问前缀（配置在application.yml）
     */
    @Value("${file.upload.local.access-prefix:http://localhost:8080/upload/}")
    private String localAccessPrefix;

    @Override
    public String uploadFile(MultipartFile file, String storagePath, String bucketName) {
        try {
            // 创建存储目录
            File targetFile = new File(localRootPath + File.separator + storagePath);
            if (!targetFile.getParentFile().exists()) {
                boolean mkdirs = targetFile.getParentFile().mkdirs();
                if (!mkdirs) {
                    log.error("创建本地存储目录失败：{}", targetFile.getParentFile().getPath());
                    return null;
                }
            }
            // 写入文件
            file.transferTo(targetFile);
            // 生成访问URL
            return localAccessPrefix + storagePath.replace(File.separator, "/");
        } catch (IOException e) {
            log.error("本地文件上传失败", e);
            return null;
        }
    }

    @Override
    public boolean uploadChunk(MultipartFile chunkFile, String chunkPath, String bucketName) {
        try {
            File chunkDir = new File(localRootPath + File.separator + chunkPath).getParentFile();
            if (!chunkDir.exists()) {
                chunkDir.mkdirs();
            }
            chunkFile.transferTo(new File(localRootPath + File.separator + chunkPath));
            return true;
        } catch (IOException e) {
            log.error("分片文件上传失败", e);
            return false;
        }
    }

    @Override
    public String mergeChunks(String uploadId, Integer totalChunks, String sourcePath, String targetPath, String bucketName) {
        try {
            // 目标文件
            File targetFile = new File(localRootPath + File.separator + targetPath);
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }

            // 合并分片
            try (FileOutputStream fos = new FileOutputStream(targetFile, true)) {
                for (int i = 0; i < totalChunks; i++) {
                    File chunkFile = new File(localRootPath + File.separator + sourcePath + File.separator + i);
                    if (!chunkFile.exists()) {
                        log.error("分片文件不存在：{}", chunkFile.getPath());
                        throw new RuntimeException("分片文件缺失，合并失败");
                    }
                    // 写入分片内容
                    Files.copy(chunkFile.toPath(), fos);
                    // 删除临时分片
                    chunkFile.delete();
                }
            }
            // 删除分片目录
            File chunkDir = new File(localRootPath + File.separator + sourcePath);
            if (chunkDir.exists()) {
                deleteDir(chunkDir);
            }
            // 生成访问URL
            return localAccessPrefix + targetPath.replace(File.separator, "/");
        } catch (Exception e) {
            log.error("分片合并失败", e);
            return null;
        }
    }

    @Override
    public InputStream getFileInputStream(String storagePath, String bucketName) {
        try {
            return Files.newInputStream(Paths.get(localRootPath + File.separator + storagePath));
        } catch (IOException e) {
            log.error("获取本地文件输入流失败", e);
            return null;
        }
    }

    @Override
    public void deleteFile(String storagePath, String bucketName) {
        File file = new File(localRootPath + File.separator + storagePath);
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public boolean exists(String storagePath, String bucketName) {
        return new File(localRootPath + File.separator + storagePath).exists();
    }

    /**
     * 删除目录及子文件
     */
    private void deleteDir(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDir(file);
                }
            }
        }
        dir.delete();
    }

    public String getLocalRootPath() {
        return localRootPath;
    }

    public void deleteDir(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDir(file);
                }
            }
        }
        dir.delete();
    }

    @Override
    public void deletePrefix(String prefix, String bucketName) {
        File dir = new File(localRootPath + File.separator + prefix);
        deleteDir(dir);
    }
}