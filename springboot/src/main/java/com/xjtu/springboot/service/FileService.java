package com.xjtu.springboot.service;

import com.xjtu.springboot.config.FileConfig;
import com.xjtu.springboot.dto.file.*;
import com.xjtu.springboot.exception.CustomException;
import com.xjtu.springboot.mapper.FileContentMapper;
import com.xjtu.springboot.mapper.FileMapper;
import com.xjtu.springboot.mapper.FileChunkMapper;
import com.xjtu.springboot.mapper.FolderMapper;
import com.xjtu.springboot.pojo.File;
import com.xjtu.springboot.pojo.FileContent;
import com.xjtu.springboot.pojo.FileChunk;
import com.xjtu.springboot.pojo.Folder;
import com.xjtu.springboot.service.storage.StorageStrategy;
import com.xjtu.springboot.util.DateUtil;
import com.xjtu.springboot.util.FileParseUtil;
import com.xjtu.springboot.util.FileUtil;
import com.xjtu.springboot.util.CommonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileConfig fileConfig;
    private final FileMapper fileMapper;
    private final FileChunkMapper fileChunkMapper;
    private final FileContentMapper fileContentMapper;
    private final FolderMapper folderMapper;
    private final FolderService folderService;
    private final Map<String, StorageStrategy> storageStrategyMap;

    public ChatFileDto chatUploadFile(MultipartFile file, ChatFileDto chatFileDto) {
        if (file.getSize() > fileConfig.getChatMaxFileSize()) {
            throw new CustomException(500, "文件大小超过100M限制");
        }
        String fileMd5 = FileUtil.calculateMd5(file);
        if (StringUtils.isEmpty(fileMd5)) {
            throw new CustomException(500, "计算文件MD5失败");
        }

        // 检查文件是否已上传
        Long userId = chatFileDto.getUserId();
        Long sessionId = chatFileDto.getSessionId();
        String anonymousId = chatFileDto.getAnonymousId();
        File existFile = fileMapper.selectByUserMd5(userId, sessionId, anonymousId, fileMd5);
        if (existFile != null) {
            chatFileDto.setFileId(existFile.getId());
            chatFileDto.setFolderId(existFile.getFolderId());
            chatFileDto.setFileMd5(existFile.getFileMd5());
            chatFileDto.setSucceed(true);
            return chatFileDto;
        }

        // 获取存储策略
        String storageType = FileUtil.getStorageType(chatFileDto.getStorageType());
        StorageStrategy storageStrategy = storageStrategyMap.get(storageType + "Storage");
        if (storageStrategy == null) {
            throw new CustomException(500, "不支持的存储类型: " + storageType);
        }

        // 查看是否有已存在的文件夹
        Folder folderData = null;
        Folder folder = folderMapper.selectByIds(userId, anonymousId, sessionId);
        if (folder != null) {
            folderData = FileUtil.generateFolderData(folder);
            folderMapper.updateByPrimaryKey(folderData);
        } else {
            folderData = FileUtil.generateFolderData(chatFileDto);
            folderMapper.insert(folderData);
        }

        File fileData = FileUtil.generateFileData(file, chatFileDto, fileMd5, storageType);
        fileMapper.insert(fileData);

        return localFile;
    }

    public boolean uploadFile(File file) {
        Long userId = file.getUserId();
        Long sessionId = file.getSessionId();
        FileFolder fileFolder = fileFolderMapper.selectByUserSessionId(userId, sessionId);

        if (Objects.isNull(fileFolder)) {
            // 创建根目录
            fileFolder = new FileFolder();
            fileFolder.setUserId(userId);
            fileFolder.setSessionId(sessionId);
            fileFolder.setFolderName("root");
            fileFolder.setParentFolderId(null);
            fileFolder.setCreatedAt(DateUtil.now());
            if (fileFolderMapper.insert(fileFolder) <= 0) {
                throw new CustomException(500, "创建文件逻辑根目录异常");
            }
        }

        file.setFolderId(fileFolder.getId());
        if (fileMapper.insert(file) <= 0) {
            throw new CustomException(500, "添加文件信息异常");

        }
        return true;
    }

    public List<File> uploadFiles(List<MultipartFile> multipartFileList, List<File> fileList) {
        List<File> files = new ArrayList<>();
        for (int i = 0; i < fileList.size(); i++) {
            files.add(uploadFile(multipartFileList.get(i), fileList.get(i)));
        }
        return files;
    }


    // 单文件上传
    @Transactional(rollbackFor = Exception.class)
    public FileResponseDto uploadSingleFile(UploadDto dto) {
        MultipartFile file = dto.getFile();
        // 1. 基础校验
        if (file.getSize() > fileConfig.getMaxFileSize()) {
            throw new CustomException(500, "文件大小超过4G限制");
        }
        String fileMd5 = FileUtil.calculateMd5(file);
        if (StringUtils.isEmpty(fileMd5)) {
            throw new CustomException(500, "计算文件MD5失败");
        }

        // 2. 检查文件是否已上传
        Long userId = dto.getUserId();
        Long sessionId = dto.getSessionId();
        String anonymousId = dto.getAnonymousId();
        File existFile = fileMapper.selectByUserMd5(userId, sessionId, anonymousId, fileMd5);
        if (existFile != null) {
            return FileResponseDto.success()
                    .setFileId(existFile.getId())
                    .setFolderId(existFile.getFolderId())
                    .setAccessUrl(existFile.getAccessUrl())
                    .setSucceed(true);
        }

        // 3. 获取存储策略
        String storageType = FileUtil.getStorageType(dto.getStorageType());
        StorageStrategy storageStrategy = storageStrategyMap.get(storageType + "Storage");
        if (storageStrategy == null) {
            throw new CustomException(500, "不支持的存储类型: " + storageType);
        }

        // 4. 上传文件到存储介质
        String newFileName = CommonUtil.getUUID() + "_" + FileUtil.getCleanedName(file);
        String storagePath = buildStoragePath(userId, anonymousId, sessionId, newFileName);
        String bucketName = dto.getBucketName();
        String accessUrl = storageStrategy.uploadFile(file, storagePath, bucketName);
        if (StringUtils.isEmpty(accessUrl)) {
            throw new CustomException(500, "文件上传异常");
        }

        // 5. 保存File主记录
        File fileData = buildFileData(dto, file, newFileName, storagePath, accessUrl, fileMd5, storageType, bucketName);
        if (fileMapper.insert(fileData) == 0) {
            // 回滚存储文件
            storageStrategy.deleteFile(storagePath, bucketName);
            throw new CustomException(500, "文件基础数据更新异常");
        }

        // 6. 解析并保存文件内容
        try {
            String content = FileParseUtil.parse(file);
            FileContent fileContent = buildFileContent(fileData, content, FileUtil.getExtension(file));
            if (fileContentMapper.insert(fileContent) == 0) {
                throw new CustomException(500, "文件内容数据更新异常");
            }
        } catch (Exception e) {
            log.error("解析文件内容失败", e);
            // 回滚File记录和存储文件
            fileMapper.deleteByPrimaryKey(fileData.getId());
            storageStrategy.deleteFile(storagePath, bucketName);
            throw new CustomException(500, "解析文件内容或更新数据库异常：" + e.getMessage());
        }

        // 7. 更新文件夹进度（如果是目录上传）
        FileResponseDto response = FileResponseDto.success()
                .setFileId(fileData.getId())
                .setSucceed(true)
                .setAccessUrl(accessUrl);
        if (dto.getFolderId() != null && StringUtils.isNotEmpty(dto.getRelativePath())) {
            folderService.updateFolderProgress(dto.getUploadId(), 1);
            response.setFolderId(dto.getFolderId());
        }
        return response;
    }

    // ===================== 多文件批量上传 =====================
    @Transactional(rollbackFor = Exception.class)
    public List<FileResponseDto> uploadMultiFiles(List<UploadDto> dtoList) {
        List<FileResponseDto> responseList = new ArrayList<>();
        for (UploadDto dto : dtoList) {
            try {
                responseList.add(uploadSingleFile(dto));
            } catch (Exception e) {
                log.error("文件上传失败：{}", dto.getFile().getOriginalFilename(), e);
                responseList.add(FileResponseDto.fail("上传失败：" + e.getMessage()));
            }
        }
        return responseList;
    }

    // ===================== 分片上传（核心） =====================
    @Transactional(rollbackFor = Exception.class)
    public FileResponseDto uploadChunk(ChunkDto dto, MultipartFile chunkFile) {
        // 1. 分片基础校验
        if (chunkFile.getSize() > fileConfig.getChunkSize() + 1024) {
            throw new CustomException(500, "分片大小超过限制（" + fileConfig.getChunkSize() / 1024 / 1024 + "MB）");
        }
        String uploadId = dto.getUploadId();
        Long userId = dto.getUserId();
        String anonymousId = dto.getAnonymousId();
        Long sessionId = dto.getSessionId();

        // 2. 获取存储策略
        String storageType = FileUtil.getStorageType(dto.getStorageType());
        StorageStrategy storageStrategy = storageStrategyMap.get(storageType + "Storage");
        if (storageStrategy == null) {
            throw new CustomException(500, "不支持的存储类型: " + storageType);
        }

        // 3. 构建分片存储路径并上传
        String chunkPath = buildChunkPath(dto);
        String bucketName = dto.getBucketName();
        boolean uploadResult = storageStrategy.uploadChunk(chunkFile, chunkPath, uploadId, dto.getChunkIndex(), bucketName);
        if (!uploadResult) {
            throw new CustomException(500, "分片" + dto.getChunkIndex() + "上传失败");
        }

        // 4. 更新FileChunk记录（核心：分片状态存储在FileChunk表）
        FileChunk fileChunk = fileChunkMapper.selectByUploadId(uploadId, userId, anonymousId);
        if (fileChunk == null) {
            // 首次上传分片，创建FileChunk记录
            fileChunk = buildFileChunk(dto, storageType, bucketName);
            fileChunkMapper.insert(fileChunk);
        } else {
            // 非首次，原子更新已上传分片数（避免并发问题）
            int updated = fileChunkMapper.incrementUploadedChunks(uploadId, userId, anonymousId);
            if (updated == 0) {
                throw new CustomException(500, "分片数更新失败（并发冲突）");
            }
            // 检查是否所有分片上传完成，更新状态为"待合并"
            fileChunk = fileChunkMapper.selectByUploadId(uploadId, userId, anonymousId);
            if (fileChunk.getUploadedChunks() == fileChunk.getTotalChunks()) {
                fileChunkMapper.updateUploadStatus(uploadId, userId, anonymousId, (short) 0, (short) 1);
            }
        }

        return FileResponseDto.success()
                .setMessage("分片" + dto.getChunkIndex() + "上传成功")
                .setSucceed(true);
    }

    // ===================== 断点续传校验 =====================
    public FileResponseDto checkBreakpoint(BreakpointCheckDTO dto) {
        // 1. 检查File是否已上传完成（直接返回结果）
        File existFile = fileMapper.selectByUserMd5(dto.getUserId(), dto.getSessionId(), dto.getAnonymousId(), dto.getFileMd5());
        if (existFile != null) {
            return FileResponseDto.success()
                    .setFileId(existFile.getId())
                    .setAccessUrl(existFile.getAccessUrl())
                    .setMessage("文件已上传完成");
        }

        // 2. 检查FileChunk分片上传状态
        FileChunk fileChunk = fileChunkMapper.selectByUploadId(dto.getUploadId(), dto.getUserId(), dto.getAnonymousId());
        if (fileChunk == null) {
            // 无分片记录，返回空列表
            return FileResponseDto.success()
                    .setUploadedChunkIndexes(new ArrayList<>())
                    .setMessage("未上传过分片");
        }

        // 3. 调用存储策略获取已上传分片（对齐OSS/本地存储的断点逻辑）
        StorageStrategy storageStrategy = storageStrategyMap.get(fileChunk.getStorageType() + "Storage");
        List<Integer> uploadedIndexes = storageStrategy.getUploadedParts(dto.getUploadId());

        return FileResponseDto.success()
                .setUploadedChunkIndexes(uploadedIndexes)
                .setMessage("已上传" + uploadedIndexes.size() + "/" + fileChunk.getTotalChunks() + "分片");
    }

    // ===================== 分片合并（核心） =====================
    @Transactional(rollbackFor = Exception.class)
    public FileResponseDto mergeChunks(String uploadId, Long userId, String anonymousId, Long sessionId, String fileMd5, String originalFileName) {
        // 1. 校验FileChunk记录
        FileChunk fileChunk = fileChunkMapper.selectByUploadId(uploadId, userId, anonymousId);
        if (fileChunk == null) {
            return FileResponseDto.fail("分片上传记录不存在");
        }
        // 校验分片是否全部上传
        if (fileChunk.getUploadedChunks() != fileChunk.getTotalChunks() || fileChunk.getUploadStatus() != 1) {
            return FileResponseDto.fail("分片未全部上传或状态异常，无法合并（当前状态：" + fileChunk.getUploadStatus() + "）");
        }

        // 2. 获取存储策略
        String storageType = fileChunk.getStorageType();
        String bucketName = fileChunk.getBucketName();
        StorageStrategy storageStrategy = storageStrategyMap.get(storageType + "Storage");
        if (storageStrategy == null) {
            return FileResponseDto.fail("不支持的存储类型：" + storageType);
        }

        // 3. 构建最终存储路径
        String newFileName = CommonUtil.getUUID() + "_" + FileUtil.getCleanedName(originalFileName);
        String targetPath = buildStoragePath(userId, anonymousId, sessionId, newFileName);
        String sourcePath = buildChunkPath(uploadId, null); // 分片根路径

        // 4. 合并分片
        String accessUrl = storageStrategy.mergeChunks(uploadId, fileChunk.getTotalChunks(), sourcePath, targetPath, bucketName);
        if (StringUtils.isEmpty(accessUrl)) {
            // 更新合并失败状态
            fileChunkMapper.updateUploadStatus(uploadId, userId, anonymousId, (short) 1, (short) 3);
            return FileResponseDto.fail("分片合并失败");
        }

        // 5. 创建File最终记录
        File fileData = new File();
        fileData.setUserId(userId);
        fileData.setAnonymousId(anonymousId);
        fileData.setSessionId(sessionId);
        fileData.setFolderId(fileChunk.getFolderId());
        fileData.setRelativePath(fileChunk.getRelativePath());
        fileData.setNewName(newFileName);
        fileData.setOriginalName(originalFileName);
        fileData.setExtension(FileUtil.getExtension(originalFileName));
        fileData.setSize(fileChunk.getFileSize()); // 从FileChunk获取文件总大小
        fileData.setStoragePath(targetPath);
        fileData.setAccessUrl(accessUrl);
        fileData.setIsImage((byte) (FileUtil.isImage(originalFileName) ? 1 : 0));
        fileData.setUploadAt(DateUtil.now());
        fileData.setFileMd5(fileMd5);
        fileData.setStorageType(storageType);
        fileData.setBucketName(bucketName);
        fileData.setMimeType(FileUtil.getMimeType(originalFileName));
        fileData.setUploadStatus((byte) 1); // 上传完成
        if (fileMapper.insert(fileData) == 0) {
            // 回滚：删除合并后的文件
            storageStrategy.deleteFile(targetPath, bucketName);
            return FileResponseDto.fail("文件记录创建失败");
        }

        // 6. 解析并保存文件内容
        try (InputStream is = storageStrategy.getFileInputStream(targetPath, bucketName)) {
            String content = FileParseUtil.parse(is, fileData.getMimeType());
            FileContent fileContent = buildFileContent(fileData, content, fileData.getExtension());
            fileContentMapper.insert(fileContent);
        } catch (Exception e) {
            log.error("解析合并后文件内容失败", e);
            // 非核心错误，不回滚主记录
        }

        // 7. 更新FileChunk状态为合并完成 + 更新文件夹进度
        fileChunkMapper.updateUploadStatus(uploadId, userId, anonymousId, (short) 1, (short) 2);
        if (fileChunk.getFolderId() != null) {
            folderService.updateFolderProgress(uploadId, 1);
        }

        return FileResponseDto.success()
                .setFileId(fileData.getId())
                .setAccessUrl(accessUrl)
                .setMessage("分片合并成功");
    }

    // ===================== 文件夹上传 =====================
    @Transactional(rollbackFor = Exception.class)
    public FileResponseDto uploadFolder(String folderUploadId, List<UploadDto> dtoList, Long userId, String anonymousId, Long sessionId) {
        List<Long> fileIds = new ArrayList<>();
        Map<String, Long> folderPathMap = new HashMap<>(); // 缓存目录路径->目录ID

        for (UploadDto dto : dtoList) {
            try {
                String relativePath = dto.getRelativePath();
                if (StringUtils.isNotEmpty(relativePath)) {
                    // 拆分目录路径，创建多级目录
                    String folderPath = relativePath.substring(0, relativePath.lastIndexOf("/"));
                    Long folderId = folderPathMap.computeIfAbsent(folderPath,
                            k -> folderService.createFolderByPath(k, userId, anonymousId, sessionId, folderUploadId));
                    dto.setFolderId(folderId);
                }
                // 上传单个文件
                FileResponseDto singleResponse = uploadSingleFile(dto);
                if (singleResponse.isSucceed()) {
                    fileIds.add(singleResponse.getFileId());
                }
            } catch (Exception e) {
                log.error("目录文件{}上传失败", dto.getFile().getOriginalFilename(), e);
            }
        }

        // 完成文件夹上传
        folderService.completeFolderUpload(folderUploadId);

        return FileResponseDto.success()
                .setFileIds(fileIds)
                .setMessage("文件夹上传完成，成功上传" + fileIds.size() + "个文件");
    }

    // ===================== 文件下载（支持断点续传） =====================
    public ResponseEntity<Resource> downloadFile(Long fileId, Long userId, String anonymousId, HttpServletRequest request, HttpServletResponse response) {
        // 1. 校验文件存在性及权限
        File file = fileMapper.selectByPrimaryKey(fileId);
        if (file == null) {
            throw new CustomException(404, "文件不存在");
        }
        boolean hasPermission = (userId != null && userId.equals(file.getUserId()))
                || (StringUtils.isNotEmpty(anonymousId) && anonymousId.equals(file.getAnonymousId()));
        if (!hasPermission) {
            throw new CustomException(403, "无权限下载该文件");
        }

        // 2. 获取存储策略并读取文件流
        StorageStrategy storageStrategy = storageStrategyMap.get(file.getStorageType() + "Storage");
        InputStream inputStream = storageStrategy.getFileInputStream(file.getStoragePath(), file.getBucketName());
        if (inputStream == null) {
            throw new CustomException(500, "文件存储介质中不存在");
        }

        // 3. 构建响应（支持断点下载）
        Resource resource = new InputStreamResource(inputStream);
        MediaType mediaType = MediaType.parseMediaType(
                StringUtils.isNotEmpty(file.getMimeType()) ? file.getMimeType() : "application/octet-stream"
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDispositionFormData("attachment", file.getOriginalName());
        headers.setContentLength(file.getSize());

        // 处理断点续传Range请求
        String range = request.getHeader("Range");
        if (StringUtils.isNotEmpty(range)) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            headers.add("Accept-Ranges", "bytes");
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    // ===================== 文件删除 =====================
    @Transactional(rollbackFor = Exception.class)
    public FileResponseDto deleteFile(Long fileId, Long userId, String anonymousId) {
        // 1. 校验文件及权限
        File file = fileMapper.selectByPrimaryKey(fileId);
        if (file == null) {
            return FileResponseDto.fail("文件不存在");
        }
        boolean hasPermission = (userId != null && userId.equals(file.getUserId()))
                || (StringUtils.isNotEmpty(anonymousId) && anonymousId.equals(file.getAnonymousId()));
        if (!hasPermission) {
            return FileResponseDto.fail("无权限删除该文件");
        }

        // 2. 删除存储介质中的文件
        StorageStrategy storageStrategy = storageStrategyMap.get(file.getStorageType() + "Storage");
        try {
            storageStrategy.deleteFile(file.getStoragePath(), file.getBucketName());
        } catch (Exception e) {
            log.error("删除存储文件失败", e);
            return FileResponseDto.fail("文件删除失败（存储介质删除异常）");
        }

        // 3. 删除数据库记录（File + FileContent）
        fileMapper.deleteByPrimaryKey(fileId);
        fileContentMapper.deleteByFileId(fileId);

        // 4. 更新文件夹文件数
        if (file.getFolderId() != null) {
            Folder folder = folderMapper.selectByPrimaryKey(file.getFolderId());
            if (folder != null && folder.getUploadedFiles() > 0) {
                folder.setUploadedFiles(folder.getUploadedFiles() - 1);
                folderMapper.updateByPrimaryKey(folder);
            }
        }

        return FileResponseDto.success().setMessage("文件删除成功");
    }

    // ===================== 文件列表查询（分页） =====================
    public PageInfo<FileVO> queryFileList(FileQueryDTO dto) {
        // 1. 分页参数处理
        int pageNum = Math.max(dto.getPageNum(), 1);
        int pageSize = Math.max(dto.getPageSize(), 20);
        int offset = (pageNum - 1) * pageSize;

        // 2. 构建查询条件
        Map<String, Object> params = new HashMap<>();
        params.put("userId", dto.getUserId());
        params.put("anonymousId", dto.getAnonymousId());
        params.put("sessionId", dto.getSessionId());
        params.put("folderId", dto.getFolderId());
        params.put("fileType", dto.getFileType());
        params.put("storageType", dto.getStorageType());
        params.put("uploadTimeStart", dto.getUploadTimeStart());
        params.put("uploadTimeEnd", dto.getUploadTimeEnd());
        params.put("offset", offset);
        params.put("pageSize", pageSize);
        params.put("orderBy", StringUtils.isEmpty(dto.getOrderBy()) ? "upload_at" : dto.getOrderBy());
        params.put("orderDir", StringUtils.isEmpty(dto.getOrderDir()) ? "desc" : dto.getOrderDir());

        // 3. 查询数据并转换VO
        List<File> fileList = fileMapper.selectByCondition(params);
        long total = fileMapper.countByCondition(params);
        List<FileVO> voList = fileList.stream().map(file -> {
            String folderName = null;
            if (file.getFolderId() != null) {
                Folder folder = folderMapper.selectByPrimaryKey(file.getFolderId());
                folderName = folder != null ? folder.getName() : null;
            }
            return FileVO.from(file, folderName);
        }).toList();

        // 4. 构建分页结果
        PageInfo<FileVO> pageInfo = new PageInfo<>();
        pageInfo.setPageNum(pageNum);
        pageInfo.setPageSize(pageSize);
        pageInfo.setTotal(total);
        pageInfo.setPages((int) Math.ceil((double) total / pageSize));
        pageInfo.setList(voList);
        return pageInfo;
    }

    // ===================== 私有工具方法 =====================

    /**
     * 构建FileChunk实体（分片上传初始化）
     */
    private FileChunk buildFileChunk(ChunkDto dto, String storageType, String bucketName) {
        FileChunk fileChunk = new FileChunk();
        fileChunk.setUploadId(dto.getUploadId());
        fileChunk.setUserId(dto.getUserId());
        fileChunk.setAnonymousId(dto.getAnonymousId());
        fileChunk.setSessionId(dto.getSessionId());
        fileChunk.setFolderId(dto.getFolderId());
        fileChunk.setRelativePath(dto.getRelativePath());
        fileChunk.setFileMd5(dto.getFileMd5());
        fileChunk.setOriginalName(dto.getOriginalFileName());
        fileChunk.setFileSize(dto.getFileSize());
        fileChunk.setTotalChunks(dto.getTotalChunks());
        fileChunk.setUploadedChunks(1);
        fileChunk.setUploadStatus((short) 0); // 0-上传中
        fileChunk.setStorageType(storageType);
        fileChunk.setBucketName(bucketName);
        fileChunk.setUploadAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
        return fileChunk;
    }

    /**
     * 构建File实体（单文件上传）
     */
    private File buildFileData(UploadDto dto, MultipartFile file, String newFileName,
                               String storagePath, String accessUrl, String fileMd5,
                               String storageType, String bucketName) {
        File fileData = new File();
        fileData.setUserId(dto.getUserId());
        fileData.setAnonymousId(dto.getAnonymousId());
        fileData.setSessionId(dto.getSessionId());
        fileData.setFolderId(dto.getFolderId());
        fileData.setNewName(newFileName);
        fileData.setOriginalName(file.getOriginalFilename());
        fileData.setExtension(FileUtil.getExtension(file));
        fileData.setSize(file.getSize());
        fileData.setStoragePath(storagePath);
        fileData.setAccessUrl(accessUrl);
        fileData.setIsImage((byte) (FileUtil.isImage(file) ? 1 : 0));
        fileData.setUploadAt(DateUtil.now());
        fileData.setFileMd5(fileMd5);
        fileData.setStorageType(storageType);
        fileData.setBucketName(bucketName);
        fileData.setMimeType(file.getContentType());
        fileData.setUploadStatus((byte) 1); // 上传完成
        fileData.setRelativePath(dto.getRelativePath());
        return fileData;
    }

    /**
     * 构建FileContent实体（文件内容解析）
     */
    private FileContent buildFileContent(File file, String content, String ext) {
        FileContent fileContent = new FileContent();
        fileContent.setFileId(file.getId());
        fileContent.setType(ext);
        fileContent.setContent(content);
        fileContent.setParseTime(DateUtil.now());
        return fileContent; // 修正原有代码缺失return的问题
    }

    /**
     * 构建最终文件存储路径
     * 规则：/[userId|anonymousId]/[sessionId]/fileName
     */
    private String buildStoragePath(Long userId, String anonymousId, Long sessionId, String fileName) {
        StringBuilder path = new StringBuilder();
        // 优先使用userId，匿名用户使用anonymousId
        if (userId != null && userId > 0) {
            path.append(userId);
        } else if (StringUtils.isNotEmpty(anonymousId)) {
            path.append(anonymousId);
        } else {
            path.append("anonymous"); // 兜底
        }
        if (sessionId != null && sessionId > 0) {
            path.append("/").append(sessionId);
        }
        return path.append("/").append(fileName).toString();
    }

    /**
     * 构建分片存储路径
     * 规则：/[userId|anonymousId]/[sessionId]/chunk/uploadId/[chunkIndex]
     */
    private String buildChunkPath(String uploadId, Integer chunkIndex) {
        StringBuilder path = new StringBuilder("chunk/").append(uploadId);
        if (chunkIndex != null) {
            path.append("/").append(chunkIndex);
        }
        return path.toString();
    }

    /**
     * 重载：通过ChunkDto构建分片路径
     */
    private String buildChunkPath(ChunkDto dto) {
        String basePath = buildStoragePath(dto.getUserId(), dto.getAnonymousId(), dto.getSessionId(), "");
        return basePath + "/" + buildChunkPath(dto.getUploadId(), dto.getChunkIndex());
    }
}