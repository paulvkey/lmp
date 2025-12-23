package com.xjtu.springboot.service.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.*;
import com.google.common.util.concurrent.RateLimiter;
import com.xjtu.springboot.pojo.FileChunk;
import com.xjtu.springboot.mapper.FileChunkPartMapper;
import com.xjtu.springboot.pojo.FileChunkPart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    /** OSS分片上传过期时间（小时），默认24小时 */
    @Value("${file.upload.oss.expire-hours:24}")
    private Integer ossExpireHours;

    // TODO
    // @Autowired
    // private FileChunkPartMapper fileChunkPartMapper;
    // @Autowired
    // private FileChunkService fileChunkService;

    // TODO 可以实现接口粒度的拦截限流
    private final RateLimiter rateLimiter = RateLimiter.create(100); // 每秒处理100个分片上传请求

    /**
     * 创建OSS客户端 TODO 优化为连接池
     */
    private OSS createOSSClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    /**
     * 关闭OSS客户端
     */
    private void closeOSSClient(OSS ossClient) {
        if (ossClient != null) {
            try {
                ossClient.shutdown();
            } catch (Exception e) {
                log.warn("关闭OSSClient失败", e);
            }
        }
    }

    // ===================== 初始化分片上传（联动FileChunk表） =====================
    @Transactional(rollbackFor = Exception.class)
    public FileChunk initChunkUpload(String uploadId, Long userId, String fileMd5,
                                     String originalName, Integer totalChunks, String targetPath,
                                     Long folderId, Long sessionId, String bucketName) {
        String bucket = bucketName == null ? defaultBucket : bucketName;
        // 1. 检查是否存在未过期的分片记录（断点续传）
        FileChunk existChunk = fileChunkService.getByUploadIdAndUserId(uploadId, userId);
        if (existChunk != null) {
            if (existChunk.getExpireAt() == null || existChunk.getExpireAt().after(new Date())) {
                log.info("OSS分片上传记录已存在，支持断点续传：uploadId={}, userId={}", uploadId, userId);
                return existChunk;
            }
            // 过期则清理OSS分片+数据库记录
            abortMultipartUpload(existChunk.getSourcePath(), existChunk.getExtraField(), bucket);
            fileChunkService.deleteById(existChunk.getId());
            fileChunkPartMapper.deleteByUploadId(uploadId);
        }

        // 2. 初始化OSS分片上传，获取原生uploadId（OSS侧标识）
        String ossUploadId = initiateMultipartUpload(targetPath, bucket);
        if (ossUploadId == null) {
            log.error("OSS初始化分片上传失败：targetPath={}, bucket={}", targetPath, bucket);
            throw new RuntimeException("OSS分片上传初始化失败");
        }

        // 3. 创建FileChunk记录
        FileChunk fileChunk = new FileChunk();
        fileChunk.setUploadId(uploadId); // 业务层uploadId
        fileChunk.setSourcePath(targetPath); // OSS的objectKey
        fileChunk.setUserId(userId);
        fileChunk.setSessionId(sessionId);
        fileChunk.setFolderId(folderId);
        fileChunk.setFileMd5(fileMd5);
        fileChunk.setOriginalName(originalName);
        fileChunk.setTotalChunks(totalChunks);
        fileChunk.setUploadedChunks(0);
        fileChunk.setUploadStatus(0); // 0-上传中
        fileChunk.setStorageType("oss");
        fileChunk.setBucketName(bucket);
        // 设置过期时间（默认24小时，可配置）
        Calendar expireCal = Calendar.getInstance();
        expireCal.add(Calendar.HOUR, ossExpireHours);
        fileChunk.setExpireAt(expireCal.getTime());
        fileChunk.setCreatedAt(new Date());
        fileChunk.setUpdatedAt(new Date());
        fileChunk.setIsDeleted(0);

        // 4. 保存记录（关联OSS原生uploadId）
        fileChunk.setExtraField(ossUploadId); // 扩展字段存储OSS的uploadId
        fileChunkService.save(fileChunk);
        log.info("OSS分片上传初始化成功：uploadId={}, ossUploadId={}, totalChunks={}",
                uploadId, ossUploadId, totalChunks);
        return fileChunk;
    }

    // ===================== OSS原生分片初始化（内部调用） =====================
    public String initiateMultipartUpload(String objectKey, String bucketName) {
        String bucket = bucketName == null ? defaultBucket : bucketName;
        OSS ossClient = null;
        try {
            ossClient = createOSSClient();
            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucket, objectKey);
            // 设置分片过期时间（OSS侧）
            request.setExpirationTime(LocalDateTime.now().plusHours(ossExpireHours));
            InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);
            return result.getUploadId();
        } catch (Exception e) {
            log.error("OSS原生分片初始化失败：objectKey={}, bucket={}", objectKey, bucket, e);
            return null;
        } finally {
            closeOSSClient(ossClient);
        }
    }

    // ===================== 分片上传（移除分布式锁，数据库层面保证并发） =====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean uploadChunk(MultipartFile chunkFile, String objectKey, String uploadId,
                               Integer partNumber, String bucketName) {
        // 限流：获取令牌失败则直接返回
        if (!rateLimiter.tryAcquire(100, TimeUnit.MILLISECONDS)) {
            log.warn("分片上传请求限流：uploadId={}", uploadId);
            return false;
        }

        String bucket = bucketName == null ? defaultBucket : bucketName;
        // 1. 校验分片上传记录是否存在
        FileChunk fileChunk = fileChunkService.getByUploadId(uploadId);
        if (fileChunk == null) {
            log.error("分片上传记录不存在：uploadId={}", uploadId);
            return false;
        }
        // 校验分片状态：仅上传中状态允许上传
        if (fileChunk.getUploadStatus() != 0) {
            log.error("分片状态异常，禁止上传：uploadId={}, status={}", uploadId, fileChunk.getUploadStatus());
            return false;
        }
        // 校验分片号合法性（OSS分片号从1开始）
        if (partNumber < 1 || partNumber > fileChunk.getTotalChunks()) {
            log.error("分片号非法：uploadId={}, partNumber={}, total={}",
                    uploadId, partNumber, fileChunk.getTotalChunks());
            return false;
        }

        // 2. 获取OSS原生uploadId
        String ossUploadId = fileChunk.getExtraField();
        if (ossUploadId == null) {
            log.error("OSS原生uploadId为空：uploadId={}", uploadId);
            return false;
        }

        // 3. 上传分片到OSS
        OSS ossClient = null;
        PartETag partETag = null;
        try {
            ossClient = createOSSClient();
            UploadPartRequest request = new UploadPartRequest();
            request.setBucketName(bucket);
            request.setKey(objectKey);
            request.setUploadId(ossUploadId);
            request.setPartNumber(partNumber);
            request.setInputStream(chunkFile.getInputStream());
            request.setPartSize(chunkFile.getSize());
            // 上传并获取ETag
            UploadPartResult result = ossClient.uploadPart(request);
            partETag = result.getPartETag();
        } catch (IOException e) {
            log.error("OSS分片上传失败：uploadId={}, partNumber={}", uploadId, partNumber, e);
            return false;
        } finally {
            closeOSSClient(ossClient);
        }

        // 4. 保存分片明细到FileChunkPart（先删后插，避免重复）
        // 核心：FileChunkPart表需加唯一索引（upload_id, part_number），防止同一分片重复插入
        fileChunkPartMapper.deleteByUploadIdAndPartNumber(uploadId, partNumber);
        FileChunkPart partEntity = new FileChunkPart();
        partEntity.setUploadId(uploadId);
        partEntity.setPartNumber(partNumber);
        partEntity.setETag(partETag.getETag());
        partEntity.setBucketName(bucket);
        partEntity.setObjectKey(objectKey);
        partEntity.setCreatedAt(new Date());
        partEntity.setIsDeleted(0);
        int insert = fileChunkPartMapper.insert(partEntity);
        if (insert <= 0) {
            log.error("保存FileChunkPart失败：uploadId={}, partNumber={}", uploadId, partNumber);
            return false;
        }

        // 5. 原子更新已上传分片数（核心：用SQL条件保证原子性，避免并发计数错误）
        // SQL示例：UPDATE file_chunk SET uploaded_chunks = uploaded_chunks + 1 WHERE upload_id = ? AND user_id = ? AND upload_status = 0
        int updated = fileChunkService.incrementUploadedChunks(uploadId, fileChunk.getUserId());
        if (updated <= 0) {
            log.error("更新已上传分片数失败（并发冲突/状态异常）：uploadId={}", uploadId);
            return false;
        }

        // 6. 检查是否所有分片上传完成
        int newUploadedChunks = fileChunk.getUploadedChunks() + 1;
        if (newUploadedChunks == fileChunk.getTotalChunks()) {
            // 仅当状态为“上传中”时更新为“待合并”（条件更新，避免并发覆盖）
            fileChunkService.updateUploadStatus(uploadId, fileChunk.getUserId(), 0, 1);
        }

        log.info("OSS分片上传成功：uploadId={}, partNumber={}, ETag={}",
                uploadId, partNumber, partETag.getETag());
        return true;
    }

    // ===================== 查询已上传分片（断点续传核心） =====================
    public List<Integer> getUploadedParts(String uploadId) {
        // 1. 从数据库查询已上传分片
        List<FileChunkPart> partEntities = fileChunkPartMapper.selectByUploadId(uploadId);
        List<Integer> dbParts = partEntities.stream()
                .map(FileChunkPart::getPartNumber)
                .sorted()
                .collect(Collectors.toList());

        // 2. 从OSS校验分片（兜底，避免数据库和OSS不一致）
        FileChunk fileChunk = fileChunkService.getByUploadId(uploadId);
        if (fileChunk == null || fileChunk.getExtraField() == null) {
            return dbParts;
        }
        String ossUploadId = fileChunk.getExtraField();
        String bucket = fileChunk.getBucketName() == null ? defaultBucket : fileChunk.getBucketName();
        OSS ossClient = null;
        try {
            ossClient = createOSSClient();
            ListPartsRequest request = new ListPartsRequest(
                    bucket, fileChunk.getSourcePath(), ossUploadId);
            PartListing partListing = ossClient.listParts(request);
            List<Integer> ossParts = partListing.getParts().stream()
                    .map(PartSummary::getPartNumber)
                    .sorted()
                    .collect(Collectors.toList());

            // 取交集：确保数据库和OSS都存在的分片才有效
            dbParts.retainAll(ossParts);
            log.info("OSS断点续传校验完成：uploadId={}, 已上传分片数={}", uploadId, dbParts.size());
            return dbParts;
        } catch (Exception e) {
            log.warn("OSS查询已上传分片失败，仅返回数据库记录：uploadId={}", uploadId, e);
            return dbParts;
        } finally {
            closeOSSClient(ossClient);
        }
    }

    // ===================== 分片合并（移除分布式锁，状态条件校验） =====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String mergeChunks(String uploadId, Integer totalChunks, String sourcePath,
                              String targetPath, String bucketName) {
        String bucket = bucketName == null ? defaultBucket : bucketName;
        // 1. 校验分片记录和状态
        FileChunk fileChunk = fileChunkService.getByUploadId(uploadId);
        if (fileChunk == null) {
            log.error("分片合并记录不存在：uploadId={}", uploadId);
            return null;
        }
        // 核心：仅当状态为“待合并（1）”且未删除时允许合并
        if (fileChunk.getUploadStatus() != 1 || fileChunk.getIsDeleted() != 0) {
            log.error("分片状态异常，不允许合并：uploadId={}, status={}", uploadId, fileChunk.getUploadStatus());
            return null;
        }

        // 2. 校验分片数量
        List<Integer> uploadedParts = getUploadedParts(uploadId);
        if (uploadedParts.size() != totalChunks) {
            log.error("OSS分片数量不匹配：uploadId={}, 预期{}，实际{}",
                    uploadId, totalChunks, uploadedParts.size());
            return null;
        }

        // 3. 准备合并的PartETag（按分片号排序，OSS要求必须有序）
        List<FileChunkPart> partEntities = fileChunkPartMapper.selectByUploadId(uploadId);
        List<PartETag> partETags = partEntities.stream()
                .sorted(Comparator.comparingInt(FileChunkPart::getPartNumber))
                .map(part -> new PartETag(part.getPartNumber(), part.getETag()))
                .collect(Collectors.toList());

        // 4. OSS合并分片
        String ossUploadId = fileChunk.getExtraField();
        OSS ossClient = null;
        try {
            ossClient = createOSSClient();
            CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(
                    bucket, targetPath, ossUploadId, partETags);
            ossClient.completeMultipartUpload(request);
        } catch (Exception e) {
            log.error("OSS分片合并失败：uploadId={}, targetPath={}", uploadId, targetPath, e);
            // 更新为合并失败状态（条件更新，避免并发覆盖）
            fileChunkService.updateUploadStatus(uploadId, fileChunk.getUserId(), 1, 3); // 3-合并失败
            return null;
        } finally {
            closeOSSClient(ossClient);
        }

        // 5. 清理分片记录
        fileChunkPartMapper.deleteByUploadId(uploadId);
        // 6. 更新FileChunk状态（条件更新：仅当当前状态为1时更新为2）
        fileChunkService.updateUploadStatus(uploadId, fileChunk.getUserId(), 1, 2); // 2-合并完成
        fileChunk.setTargetPath(targetPath);
        fileChunkService.updateById(fileChunk);

        // 7. 生成访问URL
        String endpointClean = endpoint.replaceAll("^http(s)?://", "");
        String accessUrl = String.format("https://%s.%s/%s", bucket, endpointClean, targetPath);
        log.info("OSS分片合并成功：uploadId={}, 访问URL={}", uploadId, accessUrl);
        return accessUrl;
    }

    // ===================== 中止分片上传 =====================
    public void abortMultipartUpload(String objectKey, String uploadId, String bucketName) {
        String bucket = bucketName == null ? defaultBucket : bucketName;
        OSS ossClient = null;
        try {
            ossClient = createOSSClient();
            AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(bucket, objectKey, uploadId);
            ossClient.abortMultipartUpload(request);
            log.info("中止OSS分片上传成功：objectKey={}, uploadId={}", objectKey, uploadId);
        } catch (Exception e) {
            log.error("中止OSS分片上传失败：objectKey={}, uploadId={}", objectKey, uploadId, e);
        } finally {
            closeOSSClient(ossClient);
        }
        // 清理数据库分片明细
        fileChunkPartMapper.deleteByUploadId(uploadId);
    }

    // ===================== 原有方法（单文件上传/下载/删除） =====================
    @Override
    public String uploadFile(MultipartFile file, String storagePath, String bucketName) {
        String bucket = bucketName == null ? defaultBucket : bucketName;
        OSS ossClient = null;
        try {
            ossClient = createOSSClient();
            // 上传文件（设置Content-Type，避免下载时类型错误）
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            PutObjectRequest request = new PutObjectRequest(bucket, storagePath, file.getInputStream(), metadata);
            ossClient.putObject(request);
            // 生成访问URL
            String endpointClean = endpoint.replaceAll("^http(s)?://", "");
            return String.format("https://%s.%s/%s", bucket, endpointClean, storagePath);
        } catch (IOException e) {
            log.error("OSS单文件上传失败：path={}, bucket={}", storagePath, bucket, e);
            return null;
        } finally {
            closeOSSClient(ossClient);
        }
    }

    @Override
    public InputStream getFileInputStream(String storagePath, String bucketName) {
        String bucket = bucketName == null ? defaultBucket : bucketName;
        OSS ossClient = createOSSClient();
        OSSObject ossObject = null;
        try {
            ossObject = ossClient.getObject(bucket, storagePath);
            // 封装流：关闭时自动清理OSS资源
            return new BufferedInputStream(ossObject.getObjectContent()) {
                @Override
                public void close() throws IOException {
                    super.close();
                    try {
                        ossObject.close();
                    } catch (Exception e) {
                        log.warn("关闭OSSObject失败", e);
                    }
                    closeOSSClient(ossClient);
                }
            };
        } catch (Exception e) {
            log.error("获取OSS文件流失败：path={}, bucket={}", storagePath, bucket, e);
            if (ossObject != null) {
                try {
                    ossObject.close();
                } catch (Exception ex) {
                    log.warn("关闭OSSObject失败", ex);
                }
            }
            closeOSSClient(ossClient);
            return null;
        }
    }

    @Override
    public void deleteFile(String storagePath, String bucketName) {
        String bucket = bucketName == null ? defaultBucket : bucketName;
        OSS ossClient = null;
        try {
            ossClient = createOSSClient();
            ossClient.deleteObject(bucket, storagePath);
            log.info("删除OSS文件成功：path={}", storagePath);
        } catch (Exception e) {
            log.error("删除OSS文件失败：path={}, bucket={}", storagePath, bucket, e);
        } finally {
            closeOSSClient(ossClient);
        }
    }

    @Override
    public boolean exists(String storagePath, String bucketName) {
        String bucket = bucketName == null ? defaultBucket : bucketName;
        OSS ossClient = null;
        try {
            ossClient = createOSSClient();
            return ossClient.doesObjectExist(bucket, storagePath);
        } catch (Exception e) {
            log.error("检查OSS文件存在性失败：path={}, bucket={}", storagePath, bucket, e);
            return false;
        } finally {
            closeOSSClient(ossClient);
        }
    }

    @Override
    public void deletePrefix(String prefix, String bucketName) {
        String bucket = bucketName == null ? defaultBucket : bucketName;
        OSS ossClient = null;
        try {
            ossClient = createOSSClient();
            ListObjectsV2Request request = new ListObjectsV2Request(bucket)
                    .withPrefix(prefix)
                    .withMaxKeys(1000);
            ListObjectsV2Result result;
            do {
                result = ossClient.listObjectsV2(request);
                if (!result.getObjectSummaries().isEmpty()) {
                    List<String> keys = result.getObjectSummaries().stream()
                            .map(OSSObjectSummary::getKey)
                            .collect(Collectors.toList());
                    ossClient.deleteObjects(new DeleteObjectsRequest(bucket).withKeys(keys).withQuiet(true));
                }
                request.setContinuationToken(result.getNextContinuationToken());
            } while (result.isTruncated());
            log.info("OSS前缀删除成功：prefix={}, bucket={}", prefix, bucket);
        } catch (Exception e) {
            log.error("OSS前缀删除失败：prefix={}, bucket={}", prefix, bucket, e);
        } finally {
            closeOSSClient(ossClient);
        }
    }

    // ===================== 定时任务：清理过期分片 =====================
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanExpiredParts() {
        // 1. 查询数据库中过期的分片记录
        LocalDateTime expireTime = LocalDateTime.now().minusHours(ossExpireHours);
        List<FileChunk> expireChunks = fileChunkService.listExpireChunks(expireTime);
        if (expireChunks.isEmpty()) {
            log.info("无过期OSS分片需要清理");
            return;
        }

        // 2. 遍历清理OSS+数据库
        for (FileChunk chunk : expireChunks) {
            try {
                // 中止OSS分片上传
                if (chunk.getExtraField() != null) {
                    abortMultipartUpload(chunk.getSourcePath(), chunk.getExtraField(), chunk.getBucketName());
                }
                // 删除FileChunk记录
                fileChunkService.deleteById(chunk.getId());
                // 删除FileChunkPart记录
                fileChunkPartMapper.deleteByUploadId(chunk.getUploadId());
            } catch (Exception e) {
                log.error("清理过期OSS分片失败：uploadId={}", chunk.getUploadId(), e);
            }
        }
        log.info("清理过期OSS分片完成，共处理{}条记录", expireChunks.size());
    }
}