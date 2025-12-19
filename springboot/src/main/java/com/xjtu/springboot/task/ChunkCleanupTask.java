package com.xjtu.springboot.task;

import com.xjtu.springboot.mapper.FileMapper;
import com.xjtu.springboot.pojo.File;
import com.xjtu.springboot.service.storage.StorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 分片超时清理任务（每天凌晨2点执行）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChunkCleanupTask {

    private final FileMapper fileMapper;
    private final Map<String, StorageStrategy> storageStrategyMap;

    /**
     * 清理上传中且超过24小时的分片
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredChunks() {
        log.info("开始执行分片超时清理任务");
        try {
            // 1. 查询上传中且超过24小时的分片记录
            LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
            Date expireTime = Date.from(twentyFourHoursAgo.atZone(ZoneId.systemDefault()).toInstant());
            List<File> chunkFiles = fileMapper.selectExpiredChunkFiles(expireTime);

            if (chunkFiles.isEmpty()) {
                log.info("无超时分片需要清理");
                return;
            }

            // 2. 遍历删除分片文件和数据库记录
            for (File filePO : chunkFiles) {
                try {
                    // 删除存储中的分片文件
                    StorageStrategy storageStrategy = storageStrategyMap.get(filePO.getStorageType() + "Storage");
                    if (storageStrategy != null) {
                        String chunkDir = "chunk/" + filePO.getUploadId();
                        // 本地存储删除目录，OSS/MinIO删除前缀
                        if ("local".equals(filePO.getStorageType())) {
                            // 本地存储删除分片目录（在LocalStorage中新增删除目录方法）
                            ((LocalStorage) storageStrategy).deleteDir(new File(storageStrategy.getLocalRootPath() + File.separator + chunkDir));
                        } else {
                            // OSS/MinIO删除前缀下的所有文件（需在对应Storage中实现）
                            storageStrategy.deletePrefix(chunkDir, filePO.getBucketName());
                        }
                    }

                    // 删除数据库记录
                    fileMapper.deleteByPrimaryKey(filePO);
                    log.info("清理超时分片：uploadId={}, fileMd5={}", filePO.getUploadId(), filePO.getFileMd5());
                } catch (Exception e) {
                    log.error("清理超时分片失败：fileId={}", filePO.getId(), e);
                }
            }

            log.info("分片超时清理任务执行完成，共清理{}条记录", chunkFiles.size());
        } catch (Exception e) {
            log.error("分片超时清理任务执行失败", e);
        }
    }
}
