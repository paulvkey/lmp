package com.xjtu.springboot.service.storage;

import com.xjtu.springboot.pojo.FileChunk;
import com.xjtu.springboot.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 本地存储实现类（支持断点续传+分片上传，与FileChunk表联动）
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
    @Value("${file.upload.local.access-prefix:http://localhost:8090/upload/}")
    private String localAccessPrefix;

    /**
     * 分片临时存储目录（相对于根目录）
     */
    private static final String CHUNK_TEMP_DIR = "chunk_temp";

    /**
     * 并发锁：避免同一uploadId的分片并发上传导致计数错误
     * 可以优化为过期自动清理的cache
     */
    private final Map<String, ReentrantLock> chunkLockMap = new HashMap<>();

    /**
     * TODO 注入FileChunk服务（操作分片进度表）
     */
    // @Autowired
    // private FileChunkService fileChunkService;

    // ===================== 新增：初始化分片上传（创建FileChunk记录） =====================
    /**
     * 初始化分片上传
     * @param uploadId 分片上传唯一标识
     * @param userId 用户ID
     * @param fileMd5 文件MD5（去重）
     * @param originalName 原始文件名
     * @param totalChunks 总分片数
     * @param folderId 文件夹ID
     * @param sessionId 会话ID
     * @return FileChunk 初始化后的分片进度记录
     */
    @Transactional(rollbackFor = Exception.class)
    public FileChunk initChunkUpload(String uploadId, Long userId, String fileMd5,
                                     String originalName, Integer totalChunks, Long folderId, Long sessionId) {
        // 检查是否已存在未完成的分片上传记录
        FileChunk existChunk = fileChunkService.getByUploadIdAndUserId(uploadId, userId);
        if (existChunk != null) {
            // 若存在且未过期，直接返回（断点续传）
            if (existChunk.getExpireAt() == null || existChunk.getExpireAt().after(new Date())) {
                log.info("分片上传记录已存在，支持断点续传：uploadId={}, userId={}", uploadId, userId);
                return existChunk;
            }
            // 若已过期，删除旧记录和临时文件
            deleteChunkTempFiles(uploadId, userId);
            fileChunkService.deleteById(existChunk.getId());
        }

        // 创建新的分片上传记录
        FileChunk fileChunk = new FileChunk();
        fileChunk.setUploadId(uploadId);
        fileChunk.setUserId(userId);
        fileChunk.setSessionId(sessionId);
        fileChunk.setFolderId(folderId);
        fileChunk.setFileMd5(fileMd5);
        fileChunk.setOriginalName(originalName);
        fileChunk.setTotalChunks(totalChunks);
        fileChunk.setUploadedChunks(0);
        fileChunk.setUploadStatus(0);
        fileChunk.setStorageType("local");
        // 设置过期时间（默认7天）
        fileChunk.setExpireAt(DateUtil.plusTime(7, ChronoUnit.DAYS));
        fileChunk.setCreateAt(DateUtil.now());
        fileChunk.setUpdateAt(DateUtil.now());
        fileChunk.setIsDeleted((byte)0);

        // 保存记录
        fileChunkService.save(fileChunk);
        log.debug("初始化分片上传成功：uploadId={}, totalChunks={}", uploadId, totalChunks);
        return fileChunk;
    }

    @Override
    public Boolean uploadChunk(MultipartFile chunkFile, String chunkPath, String uploadId,
                               Integer partNumber, String bucketName) {
        // 1. 解析userId（chunkPath格式：{CHUNK_TEMP_DIR}/{uploadId}/{userId}/{partNumber}）
        String[] pathParts = chunkPath.split(File.separator);
        if (pathParts.length < 4) {
            log.error("分片路径格式错误：{}", chunkPath);
            return false;
        }
        Long userId = Long.valueOf(pathParts[2]);
        ReentrantLock lock = chunkLockMap.computeIfAbsent(uploadId + "_" + userId, k -> new ReentrantLock());

        try {
            lock.lock(); // 加锁避免并发更新
            // 2. 检查分片上传记录是否存在
            FileChunk fileChunk = fileChunkService.getByUploadIdAndUserId(uploadId, userId);
            if (fileChunk == null) {
                log.error("分片上传记录不存在：uploadId={}, userId={}", uploadId, userId);
                return false;
            }

            // 3. 校验分片号合法性
            if (partNumber < 0 || partNumber >= fileChunk.getTotalChunks()) {
                log.error("分片号非法：partNumber={}, totalChunks={}", partNumber, fileChunk.getTotalChunks());
                return false;
            }

            // 4. 写入分片文件
            String absoluteChunkPath = localRootPath + File.separator + chunkPath;
            File chunkFileObj = new File(absoluteChunkPath);
            if (!chunkFileObj.getParentFile().exists()) {
                if (!chunkFileObj.getParentFile().mkdirs()) {
                    log.error("创建分片目录失败：{}", chunkFileObj.getParentFile().getPath());
                    return false;
                }
            }
            chunkFile.transferTo(chunkFileObj);

            // 5. 原子更新已上传分片数（避免并发覆盖）
            int updated = fileChunkService.incrementUploadedChunks(uploadId, userId);
            if (updated > 0) {
                log.info("分片上传成功：uploadId={}, partNumber={}, 已上传分片数={}",
                        uploadId, partNumber, fileChunk.getUploadedChunks() + 1);
                // 若所有分片上传完成，更新状态为“待合并”
                if (fileChunk.getUploadedChunks() + 1 == fileChunk.getTotalChunks()) {
                    fileChunkService.updateUploadStatus(uploadId, userId, 1); // 1-上传完成（待合并）
                }
                return true;
            } else {
                log.error("更新已上传分片数失败：uploadId={}, userId={}", uploadId, userId);
                // 回滚：删除已上传的分片文件
                chunkFileObj.delete();
                return false;
            }
        } catch (IOException e) {
            log.error("分片文件写入失败：uploadId={}, partNumber={}", uploadId, partNumber, e);
            return false;
        } finally {
            lock.unlock(); // 释放锁
        }
    }

    // ===================== 新增：查询已上传分片（断点续传核心） =====================
    /**
     * 查询已上传的分片号列表
     * @param uploadId 分片上传唯一标识
     * @param userId 用户ID
     * @return 已上传的分片号列表（升序）
     */
    public List<Integer> getUploadedParts(String uploadId, Long userId) {
        List<Integer> uploadedParts = new ArrayList<>();
        // 1. 查分片上传记录
        FileChunk fileChunk = fileChunkService.getByUploadIdAndUserId(uploadId, userId);
        if (fileChunk == null) {
            return uploadedParts;
        }

        // 2. 遍历分片临时目录，获取已存在的分片文件
        String chunkTempPath = localRootPath + File.separator + CHUNK_TEMP_DIR +
                File.separator + uploadId + File.separator + userId;
        File chunkDir = new File(chunkTempPath);
        if (!chunkDir.exists()) {
            return uploadedParts;
        }

        File[] chunkFiles = chunkDir.listFiles((dir, name) -> {
            try {
                // 分片文件名就是分片号（数字）
                Integer.parseInt(name);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        });

        if (chunkFiles != null) {
            for (File file : chunkFiles) {
                uploadedParts.add(Integer.parseInt(file.getName()));
            }
        }
        // 排序后返回
        Collections.sort(uploadedParts);
        log.info("查询已上传分片：uploadId={}, userId={}, 已上传分片数={}",
                uploadId, userId, uploadedParts.size());
        return uploadedParts;
    }

    // ===================== 改造：分片合并（校验完整性+联动FileChunk） =====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String mergeChunks(String uploadId, Integer totalChunks, String sourcePath,
                              String targetPath, String bucketName) {
        // 1. 解析userId（sourcePath格式：{CHUNK_TEMP_DIR}/{uploadId}/{userId}）
        String[] pathParts = sourcePath.split(File.separator);
        if (pathParts.length < 3) {
            log.error("分片源路径格式错误：{}", sourcePath);
            return null;
        }
        Long userId = Long.valueOf(pathParts[2]);

        // 2. 校验分片上传记录
        FileChunk fileChunk = fileChunkService.getByUploadIdAndUserId(uploadId, userId);
        if (fileChunk == null || fileChunk.getUploadStatus() != 1) {
            log.error("分片上传记录不存在或状态异常：uploadId={}, userId={}", uploadId, userId);
            return null;
        }

        // 3. 校验所有分片是否上传完成
        List<Integer> uploadedParts = getUploadedParts(uploadId, userId);
        if (uploadedParts.size() != totalChunks) {
            log.error("分片未全部上传：uploadId={}, 已上传={}, 总分片={}",
                    uploadId, uploadedParts.size(), totalChunks);
            throw new RuntimeException("分片缺失，合并失败");
        }

        // 4. 合并分片到目标文件
        File targetFile = new File(localRootPath + File.separator + targetPath);
        if (!targetFile.getParentFile().exists()) {
            if (!targetFile.getParentFile().mkdirs()) {
                log.error("创建目标文件目录失败：{}", targetFile.getParentFile().getPath());
                return null;
            }
        }

        try (FileOutputStream fos = new FileOutputStream(targetFile, true)) {
            // 按分片号顺序合并
            for (int i = 0; i < totalChunks; i++) {
                String chunkFilePath = localRootPath + File.separator + sourcePath + File.separator + i;
                File chunkFile = new File(chunkFilePath);
                if (!chunkFile.exists()) {
                    log.error("分片文件缺失：{}", chunkFilePath);
                    throw new RuntimeException("分片文件缺失，合并失败");
                }
                // 写入分片内容
                Files.copy(chunkFile.toPath(), fos);
                // 删除临时分片文件
                chunkFile.delete();
            }
        } catch (Exception e) {
            log.error("分片合并失败：uploadId={}", uploadId, e);
            // 回滚：更新合并失败状态
            fileChunkService.updateUploadStatus(uploadId, userId, 3); // 3-上传失败
            return null;
        }

        // 5. 删除分片临时目录
        deleteChunkTempFiles(uploadId, userId);

        // 6. 更新FileChunk状态为“合并完成”
        fileChunkService.updateUploadStatus(uploadId, userId, 2); // 2-合并完成
        fileChunk.setFileId(null); // 后续关联文件主表ID
        fileChunkService.updateById(fileChunk);

        // 7. 生成访问URL
        String accessUrl = localAccessPrefix + targetPath.replace(File.separator, "/");
        log.info("分片合并成功：uploadId={}, 目标路径={}, 访问URL={}", uploadId, targetPath, accessUrl);
        return accessUrl;
    }

    @Override
    public String uploadFile(MultipartFile file, String storagePath, String bucketName) {
        try {
            // 创建存储目录
            File targetFile = new File(localRootPath + File.separator + storagePath.replace("/", File.separator));
            if (!targetFile.getParentFile().exists()) {
                if (!targetFile.getParentFile().mkdirs()) {
                    log.error("创建本地存储目录失败：{}", targetFile.getParentFile().getPath());
                    return null;
                }
            }
            // 写入文件
            file.transferTo(targetFile);
            // 生成访问URL
            String accessUrl = localAccessPrefix + storagePath.replace(File.separator, "/");
            log.debug("文件上传成功：存储路径={}, 访问URL={}", storagePath, accessUrl);
            return accessUrl;
        } catch (IOException e) {
            log.error("本地文件上传失败", e);
            return null;
        }
    }

    @Override
    public InputStream getFileInputStream(String storagePath, String bucketName) {
        try {
            String absolutePath = localRootPath + File.separator + storagePath;
            if (!new File(absolutePath).exists()) {
                log.error("文件不存在：{}", absolutePath);
                return null;
            }
            return Files.newInputStream(Paths.get(absolutePath));
        } catch (IOException e) {
            log.error("获取本地文件输入流失败", e);
            return null;
        }
    }

    @Override
    public void deleteFile(String storagePath, String bucketName) {
        File file = new File(localRootPath + File.separator + storagePath);
        if (file.exists()) {
            if (file.delete()) {
                log.info("删除文件成功：{}", storagePath);
            } else {
                log.error("删除文件失败：{}", storagePath);
            }
        }
    }

    @Override
    public boolean exists(String storagePath, String bucketName) {
        return new File(localRootPath + File.separator + storagePath).exists();
    }

    @Override
    public void deletePrefix(String prefix, String bucketName) {
        File dir = new File(localRootPath + File.separator + prefix);
        deleteDir(dir);
    }

    // ===================== 私有工具方法 =====================
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
        if (dir.delete()) {
            log.info("删除目录/文件成功：{}", dir.getPath());
        } else {
            log.warn("删除目录/文件失败：{}", dir.getPath());
        }
    }

    /**
     * 删除分片临时文件
     */
    private void deleteChunkTempFiles(String uploadId, Long userId) {
        String chunkTempPath = localRootPath + File.separator + CHUNK_TEMP_DIR +
                File.separator + uploadId + File.separator + userId;
        deleteDir(new File(chunkTempPath));
    }

    /**
     * 生成分片文件的存储路径
     */
    public String generateChunkPath(String uploadId, Long userId, Integer partNumber) {
        return String.format("%s%s%s%s%s",
                CHUNK_TEMP_DIR,
                File.separator,
                uploadId,
                File.separator,
                userId,
                File.separator,
                partNumber);
    }
}