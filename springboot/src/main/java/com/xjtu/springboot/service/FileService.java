package com.xjtu.springboot.service;

import com.xjtu.springboot.config.FileConfig;
import com.xjtu.springboot.dto.file.BreakpointCheckDTO;
import com.xjtu.springboot.dto.file.ChunkUploadDTO;
import com.xjtu.springboot.dto.file.UploadDto;
import com.xjtu.springboot.dto.file.UploadResponseDTO;
import com.xjtu.springboot.exception.CustomException;
import com.xjtu.springboot.mapper.FileContentMapper;
import com.xjtu.springboot.mapper.FileMapper;
import com.xjtu.springboot.pojo.File;
import com.xjtu.springboot.pojo.FileContent;
import com.xjtu.springboot.service.storage.StorageStrategy;
import com.xjtu.springboot.util.DateUtil;
import com.xjtu.springboot.util.FileUtil;
import com.xjtu.springboot.util.CommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

    private final FileMapper fileMapper;
    private final FolderService folderService;
    private final Map<String, StorageStrategy> storageStrategyMap;
    private final FileConfig fileConfig;
    private final FileContentMapper fileContentMapper;

    @Transactional(rollbackFor = Exception.class)
    public UploadResponseDTO uploadSingleFile(UploadDto dto) {
        MultipartFile file = dto.getFile();
        // 校验文件大小
        if (file.getSize() > fileConfig.getMaxFileSize()) {
            throw new CustomException(500, "文件大小超过4G限制");
        }
        // 生成文件MD5
        String fileMd5 = FileUtil.calculateMd5(file);
        if (StringUtils.isEmpty(fileMd5)) {
            throw new CustomException(500, "计算文件MD5失败");
        }

        // 检查是否已上传
        Long userId = dto.getUserId();
        Long sessionId = dto.getSessionId();
        String anonymId = dto.getAnonymId();
        File existFile = fileMapper.selectByUserMd5(userId, sessionId, anonymId, fileMd5);
        if (existFile != null) {
            UploadResponseDTO response = UploadResponseDTO.success();
            response.setFileId(existFile.getId());
            response.setAccessUrl(existFile.getAccessUrl());
            return response;
        }

        // 获取存储策略
        String storageType = FileUtil.getStorageType(dto.getStorageType());
        StorageStrategy storageStrategy = storageStrategyMap.get(storageType + "Storage");
        if (storageStrategy == null) {
            throw new CustomException(500, "不支持的存储类型: " + storageType);
        }

        // 上传文件
        String newFileName = CommonUtil.getUUID() + "_" + FileUtil.getCleanedName(file);
        String storagePath = buildStoragePath(userId, anonymId, sessionId, newFileName);
        String bucketName = dto.getBucketName();
        String accessUrl = storageStrategy.uploadFile(file, storagePath, bucketName);
        if (StringUtils.isEmpty(accessUrl)) {
            throw new CustomException(500, "文件上传异常");
        }

        // 保存文件记录
        File fileData = buildFileData(dto, file, newFileName, storagePath, accessUrl, fileMd5, storageType, bucketName);
        if (fileMapper.insert(fileData) == 0) {
            throw new CustomException(500, "文件基础数据更新异常");
        }

        // 解析文件内容
        String content = FileContentParser.parseContent(file);
        FileContent fileContent = buildFileContent(fileData, content);
        if (fileContentMapper.insert(fileContent) == 0) {
            throw new CustomException(500, "文件内容数据更新异常");
        }

        // 更新文件夹进度（如果是目录上传）
        if (dto.getFolderId() != null && StringUtils.isNotEmpty(dto.getRelativePath())) {
            folderService.updateFolderProgress(fileData.getUploadId(), fileData.getUploadedChunks() + 1);
        }

        UploadResponseDTO response = UploadResponseDTO.success();
        response.setFileId(fileData.getId());
        response.setAccessUrl(accessUrl);
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public UploadResponseDTO uploadMultipleFiles(List<UploadDto> dtoList) {
        List<Long> fileIds = new ArrayList<>();
        List<String> failFiles = new ArrayList<>();

        for (UploadDto dto : dtoList) {
            try {
                UploadResponseDTO singleResponse = uploadSingleFile(dto);
                if (singleResponse.getCode() == 200) {
                    fileIds.add(singleResponse.getFileId());
                } else {
                    failFiles.add(dto.getFile().getOriginalFilename());
                }
            } catch (Exception e) {
                log.error("多文件上传失败：{}", dto.getFile().getOriginalFilename(), e);
                failFiles.add(dto.getFile().getOriginalFilename());
            }
        }

        UploadResponseDTO response = UploadResponseDTO.success();
        if (!failFiles.isEmpty()) {
            response.setCode(206);
            response.setMessage("部分文件上传失败：" + String.join(",", failFiles));
        }
        response.setFileIds(fileIds);
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public UploadResponseDTO uploadChunk(ChunkUploadDTO dto) {
        MultipartFile chunkFile = dto.getChunkFile();
        // 校验分片大小
        if (chunkFile.getSize() > chunkSize + 1024) { // 允许1KB误差
            return UploadResponseDTO.fail("分片大小超过限制");
        }

        // 处理存储路径
        String storageType = StringUtils.isEmpty(dto.getStorageType()) ? "local" : dto.getStorageType();
        String bucketName = dto.getBucketName();
        // 分片存储路径：uploadId/分片索引
        String chunkPath = buildChunkPath(dto.getUploadId(), dto.getChunkIndex());

        // 获取存储策略
        StorageStrategy storageStrategy = storageStrategyMap.get(storageType + "Storage");
        if (storageStrategy == null) {
            return UploadResponseDTO.fail("不支持的存储类型：" + storageType);
        }

        // 上传分片
        boolean uploadResult = storageStrategy.uploadChunk(chunkFile, chunkPath, bucketName);
        if (!uploadResult) {
            return UploadResponseDTO.fail("分片上传失败");
        }

        // 更新文件分片记录
        File file = fileMapper.selectByUploadId(dto.getUploadId(), dto.getUserId(), dto.getAnonymId());
        if (file == null) {
            // 首次上传分片，创建文件记录
            file = new File();
            file.setUserId(dto.getUserId());
            file.setAnonymId(dto.getAnonymId());
            file.setSessionId(dto.getSessionId());
            file.setFolderId(dto.getFolderId());
            file.setRelativePath(dto.getRelativePath());
            file.setUploadId(dto.getUploadId());
            file.setFileMd5(dto.getFileMd5());
            file.setOriginalName(dto.getOriginalFileName());
            file.setTotalChunks(dto.getTotalChunks());
            file.setUploadedChunks(1);
            file.setUploadStatus((short) 0); // 上传中
            file.setStorageType(storageType);
            file.setBucketName(bucketName);
            file.setUploadAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
            fileMapper.insert(file);
        } else {
            // 更新已上传分片数
            file.setUploadedChunks(file.getUploadedChunks() + 1);
            fileMapper.updateByPrimaryKey(file);
        }

        UploadResponseDTO response = UploadResponseDTO.success();
        response.setMessage("分片" + dto.getChunkIndex() + "上传成功");
        return response;
    }

    public UploadResponseDTO checkBreakpoint(BreakpointCheckDTO dto) {
        // 查询文件记录
        File file = fileMapper.selectByUserMd5(dto.getUserId(), dto.getAnonymId(), dto.getFileMd5());
        if (file == null) {
            // 未上传过，返回空列表
            UploadResponseDTO response = UploadResponseDTO.success();
            response.setUploadedChunkIndexes(new ArrayList<>());
            return response;
        }

        // 已上传完成
        if (file.getUploadStatus() == 1) {
            UploadResponseDTO response = UploadResponseDTO.success();
            response.setFileId(file.getId());
            response.setAccessUrl(file.getAccessUrl());
            response.setMessage("文件已上传完成");
            return response;
        }

        // 校验已上传分片
        String storageType = file.getStorageType();
        String bucketName = file.getBucketName();
        StorageStrategy storageStrategy = storageStrategyMap.get(storageType + "Storage");
        List<Integer> uploadedIndexes = new ArrayList<>();

        for (int i = 0; i < file.getTotalChunks(); i++) {
            String chunkPath = buildChunkPath(dto.getUploadId(), i);
            if (storageStrategy.exists(chunkPath, bucketName)) {
                uploadedIndexes.add(i);
            }
        }

        UploadResponseDTO response = UploadResponseDTO.success();
        response.setUploadedChunkIndexes(uploadedIndexes);
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public UploadResponseDTO mergeChunks(String uploadId, Long userId, String anonymId, Long sessionId, String fileMd5, String originalFileName) {
        // 查询文件记录
        File file = fileMapper.selectByUploadId(uploadId, userId, anonymId);
        if (file == null) {
            return UploadResponseDTO.fail("分片上传记录不存在");
        }

        // 校验分片数
        if (file.getUploadedChunks() != file.getTotalChunks()) {
            return UploadResponseDTO.fail("分片未全部上传，无法合并");
        }

        // 处理存储路径
        String storageType = file.getStorageType();
        String bucketName = file.getBucketName();
        String newFileName = CommonUtil.generateUUID() + "_" + originalFileName.replaceAll("[^a-zA-Z0-9\\.]+", "_");
        String targetPath = buildStoragePath(userId, anonymId, sessionId, newFileName);
        String sourcePath = buildChunkPath(uploadId, null); // 分片目录

        // 获取存储策略
        StorageStrategy storageStrategy = storageStrategyMap.get(storageType + "Storage");
        if (storageStrategy == null) {
            return UploadResponseDTO.fail("不支持的存储类型：" + storageType);
        }

        // 合并分片
        String accessUrl = storageStrategy.mergeChunks(uploadId, file.getTotalChunks(), sourcePath, targetPath, bucketName);
        if (StringUtils.isEmpty(accessUrl)) {
            return UploadResponseDTO.fail("分片合并失败");
        }

        // 更新文件记录
        file.setNewName(newFileName);
        file.setStoragePath(targetPath);
        file.setAccessUrl(accessUrl);
        file.setSize(file.getFileSize());
        file.setUploadStatus((short) 1); // 上传完成
        // 解析文件内容
        try (InputStream is = storageStrategy.getFileInputStream(targetPath, bucketName)) {
            String content = FileContentParser.parseContent(is, file.getMimeType());
            file.setContent(content);
        } catch (Exception e) {
            log.error("解析合并后的文件内容失败", e);
        }
        fileMapper.updateByPrimaryKey(file);

        // 更新文件夹进度
        if (file.getFolderId() != null) {
            folderService.updateFolderProgress(file.getUploadId(), file.getUploadedChunks() + 1);
        }

        UploadResponseDTO response = UploadResponseDTO.success();
        response.setFileId(file.getId());
        response.setAccessUrl(accessUrl);
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public UploadResponseDTO uploadFolder(String folderUploadId, List<UploadDto> dtoList, Long userId, String anonymId, Long sessionId) {
        // 批量创建文件+目录
        List<Long> fileIds = new ArrayList<>();
        Map<String, Long> folderPathMap = new HashMap<>(); // 缓存目录路径->目录ID

        for (UploadDto dto : dtoList) {
            try {
                String relativePath = dto.getRelativePath();
                if (StringUtils.isEmpty(relativePath)) {
                    // 根目录文件
                    dto.setFolderId(null);
                } else {
                    // 拆分目录和文件名
                    String folderPath = relativePath.substring(0, relativePath.lastIndexOf("/"));
                    Long folderId;
                    if (folderPathMap.containsKey(folderPath)) {
                        folderId = folderPathMap.get(folderPath);
                    } else {
                        folderId = folderService.createFolderByPath(folderPath, userId, anonymId, sessionId, folderUploadId);
                        folderPathMap.put(folderPath, folderId);
                    }
                    dto.setFolderId(folderId);
                }
                // 上传文件
                UploadResponseDTO singleResponse = uploadSingleFile(dto);
                if (singleResponse.getCode() == 200) {
                    fileIds.add(singleResponse.getFileId());
                }
            } catch (Exception e) {
                log.error("目录文件上传失败", e);
                continue;
            }
        }

        // 完成文件夹上传
        folderService.completeFolderUpload(folderUploadId);

        UploadResponseDTO response = UploadResponseDTO.success();
        response.setFileIds(fileIds);
        return response;
    }

    public String parseFileContent(Long fileId) {
        File file = fileMapper.selectByPrimaryKey(fileId);
        if (file == null) {
            log.error("文件不存在：{}", fileId);
            return null;
        }

        // 获取存储策略
        StorageStrategy storageStrategy = storageStrategyMap.get(file.getStorageType() + "Storage");
        if (storageStrategy == null) {
            log.error("不支持的存储类型：{}", file.getStorageType());
            return null;
        }

        // 获取文件输入流并解析
        try (InputStream is = storageStrategy.getFileInputStream(file.getStoragePath(), file.getBucketName())) {
            return FileContentParser.parseContent(is, file.getMimeType());
        } catch (Exception e) {
            log.error("解析文件内容失败", e);
            return null;
        }
    }

    /**
     * 构建文件存储路径
     */
    private String buildStoragePath(Long userId, String anonymId, Long sessionId, String fileName) {
        // 存储路径规则：user/[userId|anonymId]/session/[sessionId]/fileName
        StringBuilder path = new StringBuilder();
        path.append("user/");
        path.append(userId != null ? userId : anonymId);
        path.append("/session/");
        path.append(sessionId != null ? sessionId : "public");
        path.append("/");
        path.append(fileName);
        return path.toString();
    }

    /**
     * 构建分片存储路径
     */
    private String buildChunkPath(String uploadId, Integer chunkIndex) {
        StringBuilder path = new StringBuilder();
        path.append("chunk/");
        path.append(uploadId);
        if (chunkIndex != null) {
            path.append("/");
            path.append(chunkIndex);
        }
        return path.toString();
    }

    /**
     * 构建File实体
     */
    private File buildFileData(UploadDto dto, MultipartFile file, String newFileName,
                               String storagePath, String accessUrl, String fileMd5,
                               String storageType, String bucketName) {
        File fileData = new File();
        fileData.setUserId(dto.getUserId());
        fileData.setAnonymId(dto.getAnonymId());
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
        fileData.setUploadStatus((byte) 1);
        fileData.setRelativePath(dto.getRelativePath());
        return fileData;
    }

    public FileContent buildFileContent(File file, String content) {
        FileContent fileContent = new FileContent();
        fileContent.setFileId(file.getId());
        fileContent.setType();
        fileContent.setContent(content);
        fileContent.setParseTime(DateUtil.now());
    }

    public ResponseEntity<Resource> downloadFile(Long fileId, Long userId, String anonymId, HttpServletRequest request, HttpServletResponse response) {
        // 1. 校验文件是否存在及权限
        File file = fileMapper.selectByPrimaryKey(fileId);
        if (file == null) {
            throw new RuntimeException("文件不存在");
        }
        // 权限校验：登录用户只能下载自己的文件，匿名用户只能下载自己上传的文件
        boolean hasPermission = (userId != null && userId.equals(file.getUserId()))
                || (StringUtils.isNotEmpty(anonymId) && anonymId.equals(file.getAnonymId()));
        if (!hasPermission) {
            throw new RuntimeException("无权限下载该文件");
        }

        // 2. 获取存储策略
        StorageStrategy storageStrategy = storageStrategyMap.get(file.getStorageType() + "Storage");
        if (storageStrategy == null) {
            throw new RuntimeException("不支持的存储类型：" + file.getStorageType());
        }

        // 3. 获取文件输入流
        InputStream inputStream = storageStrategy.getFileInputStream(file.getStoragePath(), file.getBucketName());
        if (inputStream == null) {
            throw new RuntimeException("文件下载失败，文件不存在于存储介质");
        }

        // 4. 处理断点下载
        String range = request.getHeader("Range");
        Resource resource = new InputStreamResource(inputStream);
        MediaType mediaType = MediaType.parseMediaType(file.getMimeType() != null ? file.getMimeType() : "application/octet-stream");

        // 5. 构建响应
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalName() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.getSize()))
                .body(resource);
    }

    public UploadResponseDTO deleteFile(Long fileId, Long userId, String anonymId) {
        // 1. 校验文件及权限
        File file = fileMapper.selectByPrimaryKey(fileId);
        if (file == null) {
            return UploadResponseDTO.fail("文件不存在");
        }
        boolean hasPermission = (userId != null && userId.equals(file.getUserId()))
                || (StringUtils.isNotEmpty(anonymId) && anonymId.equals(file.getAnonymId()));
        if (!hasPermission) {
            return UploadResponseDTO.fail("无权限删除该文件");
        }

        // 2. 删除存储文件
        StorageStrategy storageStrategy = storageStrategyMap.get(file.getStorageType() + "Storage");
        try {
            storageStrategy.deleteFile(file.getStoragePath(), file.getBucketName());
        } catch (Exception e) {
            log.error("删除存储文件失败", e);
            return UploadResponseDTO.fail("文件删除失败");
        }

        // 3. 删除数据库记录
        fileMapper.deleteByPrimaryKey(file);

        // 4. 更新文件夹文件数（如果文件属于文件夹）
        if (file.getFolderId() != null) {
            Folder folder = folderMapper.selectByPrimaryKey(file.getFolderId());
            if (folder != null && folder.getUploadedFiles() > 0) {
                folder.setUploadedFiles(folder.getUploadedFiles() - 1);
                folderMapper.updateByPrimaryKey(folder);
            }
        }

        return UploadResponseDTO.success();
    }

    public PageInfo<FileVO> queryFileList(FileQueryDTO dto) {
        // 1. 分页参数处理
        int pageNum = dto.getPageNum() < 1 ? 1 : dto.getPageNum();
        int pageSize = dto.getPageSize() < 1 ? 20 : dto.getPageSize();
        int offset = (pageNum - 1) * pageSize;

        // 2. 构建查询条件
        Map<String, Object> params = new HashMap<>();
        params.put("userId", dto.getUserId());
        params.put("anonymId", dto.getAnonymId());
        params.put("sessionId", dto.getSessionId());
        params.put("folderId", dto.getFolderId());
        params.put("fileType", dto.getFileType());
        params.put("storageType", dto.getStorageType());
        params.put("uploadTimeStart", dto.getUploadTimeStart());
        params.put("uploadTimeEnd", dto.getUploadTimeEnd());
        params.put("offset", offset);
        params.put("pageSize", pageSize);
        params.put("orderBy", dto.getOrderBy());
        params.put("orderDir", dto.getOrderDir());

        // 3. 查询数据
        List<File> fileList = fileMapper.selectByCondition(params);
        long total = fileMapper.countByCondition(params);

        // 4. 转换VO（关联文件夹名称）
        List<FileVO> voList = new ArrayList<>();
        for (File file : fileList) {
            String folderName = null;
            if (file.getFolderId() != null) {
                Folder folder = folderMapper.selectByPrimaryKey(file.getFolderId());
                if (folder != null) {
                    folderName = folder.getName();
                }
            }
            voList.add(FileVO.from(file, folderName));
        }

        // 5. 构建分页结果
        PageInfo<FileVO> pageInfo = new PageInfo<>();
        pageInfo.setPageNum(pageNum);
        pageInfo.setPageSize(pageSize);
        pageInfo.setTotal(total);
        pageInfo.setPages((int) (total + pageSize - 1) / pageSize);
        pageInfo.setList(voList);
        return pageInfo;
    }
}