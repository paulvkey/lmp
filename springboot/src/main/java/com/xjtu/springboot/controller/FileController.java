package com.xjtu.springboot.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjtu.springboot.common.Result;
import com.xjtu.springboot.dto.file.*;
import com.xjtu.springboot.dto.file.FileVO;
import com.xjtu.springboot.pojo.File;
import com.xjtu.springboot.service.FileService;
import com.xjtu.springboot.service.FolderService;
import com.xjtu.springboot.util.CommonUtil;
import com.xjtu.springboot.util.FileUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FolderService folderService;

    @RequestMapping(method = RequestMethod.POST, path = "chat/file/upload")
    public Result chatUploadFile(@RequestParam("requestParams") String requestParams,
                                 @RequestParam("file") MultipartFile file) {
        try {
            ChatFileDto chatFileDto = new ObjectMapper().readValue(
                    requestParams,
                    new TypeReference<ChatFileDto>() {
                    }
            );
            return Result.success(fileService.chatUploadFile(file, chatFileDto));
        } catch (JsonProcessingException e) {
            return Result.error("文件信息解析失败：" + e.getMessage());
        }
    }

    @RequestMapping(method = RequestMethod.POST, path = "chat/files/upload")
    public Result chatUploadFiles(@RequestParam("multipartFileList") List<MultipartFile> multipartFileList,
                                  @RequestParam("fileListJson") String fileListJson) {
        try {
            List<File> fileList = new ObjectMapper().readValue(
                    fileListJson,
                    new TypeReference<List<File>>() {
                    }
            );
            return Result.success(fileService.uploadFiles(multipartFileList, fileList));
        } catch (JsonProcessingException e) {
            return Result.error("文件信息解析失败：" + e.getMessage());
        }
    }

    /**
     * 单文件上传（支持登录/匿名用户，关联会话/文件夹）
     *
     * @param userId      登录用户ID（二选一：userId/anonymousId）
     * @param anonymousId 匿名用户临时ID
     * @param file        上传的文件（必填）
     */
    @PostMapping("/single/upload")
    public Result uploadSingleFile(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String anonymousId,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) String storageType,
            @RequestParam(required = false) String bucketName,
            @RequestParam("file") MultipartFile file) {
        try {
            // 基础参数校验：userId/anonymousId二选一，文件非空
            if (!FileUtil.checkParams(userId, anonymousId)) {
                return Result.error("用户ID/匿名ID不能为空");
            }
            if (file == null || file.isEmpty()) {
                return Result.error("上传文件不能为空");
            }

            UploadDto dto = generateUploadDto(userId, anonymousId, sessionId, folderId, storageType, bucketName, file);
            FileResponseDto responseDto = fileService.uploadSingleFile(dto);
            return Result.success(responseDto);
        } catch (Exception e) {
            log.error("单文件上传失败", e);
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    // ===================== 多文件批量上传 =====================

    /**
     * 多文件批量上传
     */
    @PostMapping("/multiple/upload")
    public Result uploadMultiFiles(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String anonymousId,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) String storageType,
            @RequestParam(required = false) String bucketName,
            @RequestParam("files") MultipartFile[] files) {
        try {
            // 参数校验
            if (!FileUtil.checkParams(userId, anonymousId)) {
                return Result.error("用户ID/匿名ID不能为空");
            }
            if (files == null || files.length == 0) {
                return Result.error("上传文件列表不能为空");
            }

            List<UploadDto> dtoList = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    log.warn("跳过空文件：{}", file.getOriginalFilename());
                    continue;
                }
                UploadDto dto = generateUploadDto(userId, anonymousId, sessionId, folderId, storageType, bucketName, file);
                dtoList.add(dto);
            }

            List<FileResponseDto> responseList = fileService.uploadMultiFiles(dtoList);
            return Result.success(responseList);
        } catch (Exception e) {
            log.error("多文件上传失败", e);
            return Result.error("批量上传失败：" + e.getMessage());
        }
    }

    // ===================== 断点续传校验 =====================

    /**
     * 断点续传校验（检查已上传分片/文件是否存在）
     */
    @PostMapping("/breakpoint/check")
    public Result checkBreakpoint(@Valid @RequestBody BreakpointCheckDTO dto) {
        try {
            // 补充参数校验
            if (StringUtils.isEmpty(dto.getFileMd5())) {
                return Result.error("文件MD5不能为空");
            }
            if (!FileUtil.checkParams(dto.getUserId(), dto.getAnonymousId())) {
                return Result.error("用户ID/匿名ID不能为空");
            }

            FileResponseDto responseDto = fileService.checkBreakpoint(dto);
            return Result.success(responseDto);
        } catch (Exception e) {
            log.error("断点续传校验失败", e);
            return Result.error("断点校验失败：" + e.getMessage());
        }
    }

    // ===================== 分片上传 =====================

    /**
     * 分片上传（大文件分片）
     *
     * @param uploadId   分片上传唯一标识（前端从/generateUploadId获取或后端OSS返回）
     * @param chunkIndex 分片索引（从0/1开始，与前端约定）
     */
    @PostMapping("/chunk/upload")
    public Result uploadChunk(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String anonymousId,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) String storageType,
            @RequestParam(required = false) String bucketName,
            @RequestParam("uploadId") String uploadId,
            @RequestParam("totalChunks") Integer totalChunks,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam("originalFileName") String originalFileName,
            @RequestParam("fileSize") Long fileSize,
            @RequestParam("chunkFile") MultipartFile chunkFile) {
        try {
            // 核心参数校验
            if (!FileUtil.checkParams(userId, anonymousId)) {
                return Result.error("用户ID/匿名ID不能为空");
            }
            if (StringUtils.isEmpty(uploadId)) {
                return Result.error("分片上传ID不能为空");
            }
            if (totalChunks == null || totalChunks <= 0) {
                return Result.error("总分片数必须大于0");
            }
            if (chunkIndex == null || chunkIndex < 0) {
                return Result.error("分片索引不能为负数");
            }
            if (StringUtils.isEmpty(fileMd5)) {
                return Result.error("文件MD5不能为空");
            }
            if (chunkFile == null || chunkFile.isEmpty()) {
                return Result.error("分片文件不能为空");
            }

            ChunkDto dto = new ChunkDto();
            dto.setUserId(userId);
            dto.setAnonymousId(anonymousId);
            dto.setSessionId(sessionId);
            dto.setFolderId(folderId);
            dto.setStorageType(storageType);
            dto.setBucketName(bucketName);
            dto.setUploadId(uploadId);
            dto.setTotalChunks(totalChunks);
            dto.setChunkIndex(chunkIndex);
            dto.setFileMd5(fileMd5);
            dto.setOriginalFileName(originalFileName);
            dto.setFileSize(fileSize);
            dto.setChunkFile(chunkFile);

            FileResponseDto responseDto = fileService.uploadChunk(dto);
            return Result.success(responseDto);
        } catch (Exception e) {
            log.error("分片{}上传失败：uploadId={}", chunkIndex, uploadId, e);
            return Result.error("分片" + chunkIndex + "上传失败：" + e.getMessage());
        }
    }

    // ===================== 分片合并 =====================

    /**
     * 分片合并（所有分片上传完成后调用）
     */
    @PostMapping("/chunk/merge")
    public Result mergeChunks(
            @RequestParam("uploadId") String uploadId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String anonymousId,
            @RequestParam(required = false) Long sessionId,
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam("originalFileName") String originalFileName) {
        try {
            // 参数校验
            if (!FileUtil.checkParams(userId, anonymousId)) {
                return Result.error("用户ID/匿名ID不能为空");
            }
            if (StringUtils.isEmpty(uploadId)) {
                return Result.error("分片上传ID不能为空");
            }
            if (StringUtils.isEmpty(fileMd5)) {
                return Result.error("文件MD5不能为空");
            }
            if (StringUtils.isEmpty(originalFileName)) {
                return Result.error("原始文件名不能为空");
            }

            FileResponseDto responseDto = fileService.mergeChunks(uploadId, userId, anonymousId, sessionId, fileMd5, originalFileName);
            return Result.success(responseDto);
        } catch (Exception e) {
            log.error("分片合并失败：uploadId={}", uploadId, e);
            return Result.error("分片合并失败：" + e.getMessage());
        }
    }

    // ===================== 文件夹上传相关 =====================

    /**
     * 初始化文件夹上传（生成文件夹级uploadId）
     */
    @PostMapping("/folder/init")
    public Result initFolderUpload(@Valid @RequestBody FolderInitDTO dto) {
        try {
            // 参数校验
            if (!FileUtil.checkParams(dto.getUserId(), dto.getAnonymousId())) {
                return Result.error("用户ID/匿名ID不能为空");
            }
            if (StringUtils.isEmpty(dto.getFolderName())) {
                return Result.error("文件夹名称不能为空");
            }
            if (StringUtils.isEmpty(dto.getFolderUploadId())) {
                return Result.error("文件夹上传ID不能为空");
            }

            Long folderId = folderService.initFolderUpload(dto);
            FileResponseDto response = FileResponseDto.success();
            response.setFolderId(folderId);
            return Result.success(response);
        } catch (Exception e) {
            log.error("文件夹上传初始化失败", e);
            return Result.error("文件夹初始化失败：" + e.getMessage());
        }
    }

    /**
     * 文件夹批量上传（含多级目录）
     * 前端需通过webkitdirectory获取文件相对路径，封装在originalFilename中
     */
    @PostMapping("/folder/upload")
    public Result uploadFolder(
            @RequestParam("folderUploadId") String folderUploadId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String anonymousId,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) String storageType,
            @RequestParam(required = false) String bucketName,
            @RequestParam("files") MultipartFile[] files) {
        try {
            // 参数校验
            if (!FileUtil.checkParams(userId, anonymousId)) {
                return Result.error("用户ID/匿名ID不能为空");
            }
            if (StringUtils.isEmpty(folderUploadId)) {
                return Result.error("文件夹上传ID不能为空");
            }
            if (files == null || files.length == 0) {
                return Result.error("文件夹内文件不能为空");
            }

            List<UploadDto> dtoList = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    log.warn("跳过空文件：{}", file.getOriginalFilename());
                    continue;
                }
                // 关键：webkitdirectory模式下，originalFilename包含完整相对路径（如 "doc/2024/test.txt"）
                String relativePath = file.getOriginalFilename();
                UploadDto dto = new UploadDto();
                dto.setUserId(userId);
                dto.setAnonymousId(anonymousId);
                dto.setSessionId(sessionId);
                dto.setStorageType(storageType);
                dto.setBucketName(bucketName);
                dto.setFile(file);
                dto.setRelativePath(relativePath);
                dtoList.add(dto);
            }

            FileResponseDto responseDto = fileService.uploadFolder(folderUploadId, dtoList, userId, anonymousId, sessionId);
            return Result.success(responseDto);
        } catch (Exception e) {
            log.error("文件夹上传失败：folderUploadId={}", folderUploadId, e);
            return Result.error("文件夹上传失败：" + e.getMessage());
        }
    }

    // ===================== 文件解析/下载/删除/查询 =====================

    /**
     * 解析文件内容（文本/图片OCR等）
     */
    @GetMapping("/content/parse")
    public Result parseFileContent(@RequestParam("fileId") Long fileId) {
        try {
            if (fileId == null || fileId <= 0) {
                return Result.error("文件ID无效");
            }
            String content = fileService.parseFileContent(fileId);
            FileResponseDto response = FileResponseDto.success();
            if (content == null) {
                response.setCode(500);
                response.setMessage("解析文件内容失败");
            } else {
                response.setMessage(content);
            }
            return Result.success(response);
        } catch (Exception e) {
            log.error("解析文件内容失败：fileId={}", fileId, e);
            return Result.error("解析失败：" + e.getMessage());
        }
    }

    /**
     * 生成上传ID（供前端获取分片/文件夹上传的唯一标识）
     * 注：OSS分片上传的uploadId需后端调用OSS接口生成，此接口仅生成前端用的临时标识
     */
    @GetMapping("/uploadId/generate")
    public Result generateUploadId() {
        try {
            String uploadId = CommonUtil.getUUID(); // 对齐工具类方法名（原generateUUID改为getUUID）
            FileResponseDto response = FileResponseDto.success();
            response.setMessage(uploadId);
            return Result.success(response);
        } catch (Exception e) {
            log.error("生成上传ID失败", e);
            return Result.error("生成上传ID失败：" + e.getMessage());
        }
    }

    /**
     * 文件下载（支持断点续传）
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(
            @RequestParam Long fileId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String anonymousId,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            if (fileId == null || fileId <= 0) {
                throw new IllegalArgumentException("文件ID无效");
            }
            if (!FileUtil.checkParams(userId, anonymousId)) {
                throw new IllegalArgumentException("用户ID/匿名ID不能为空");
            }
            return fileService.downloadFile(fileId, userId, anonymousId, request, response);
        } catch (Exception e) {
            log.error("文件下载失败：fileId={}", fileId, e);
            // 抛出运行时异常，由全局异常处理器处理（或直接返回400/500）
            throw new RuntimeException("下载失败：" + e.getMessage());
        }
    }

    /**
     * 删除单个文件（软删除）
     */
    @DeleteMapping("/delete")
    public Result deleteFile(
            @RequestParam Long fileId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String anonymousId) {
        try {
            if (fileId == null || fileId <= 0) {
                return Result.error("文件ID无效");
            }
            if (!FileUtil.checkParams(userId, anonymousId)) {
                return Result.error("用户ID/匿名ID不能为空");
            }
            FileResponseDto responseDto = fileService.deleteFile(fileId, userId, anonymousId);
            return Result.success(responseDto);
        } catch (Exception e) {
            log.error("删除文件失败：fileId={}", fileId, e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 级联删除文件夹（含子文件夹/文件）
     */
    @DeleteMapping("/folder/delete")
    public Result deleteFolder(
            @RequestParam Long folderId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String anonymousId) {
        try {
            if (folderId == null || folderId <= 0) {
                return Result.error("文件夹ID无效");
            }
            if (!FileUtil.checkParams(userId, anonymousId)) {
                return Result.error("用户ID/匿名ID不能为空");
            }
            FileResponseDto responseDto = folderService.deleteFolder(folderId, userId, anonymousId);
            return Result.success(responseDto);
        } catch (Exception e) {
            log.error("删除文件夹失败：folderId={}", folderId, e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 文件列表分页查询（支持多条件筛选）
     */
    @PostMapping("/list")
    public Result<PageInfo<FileVO>> queryFileList(@Valid @RequestBody FileQueryDTO dto) {
        try {
            // 分页参数默认值处理
            if (dto.getPageNum() == null || dto.getPageNum() < 1) {
                dto.setPageNum(1);
            }
            if (dto.getPageSize() == null || dto.getPageSize() < 1) {
                dto.setPageSize(20);
            }
            if (!FileUtil.checkParams(dto.getUserId(), dto.getAnonymousId())) {
                return Result.error("用户ID/匿名ID不能为空");
            }

            PageInfo<FileVO> pageInfo = fileService.queryFileList(dto);
            return Result.success(pageInfo);
        } catch (Exception e) {
            log.error("查询文件列表失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    // ===================== 私有工具方法 =====================

    /**
     * 构建UploadDto（复用单文件/多文件上传的DTO构建逻辑）
     */
    private UploadDto generateUploadDto(Long userId, String anonymousId, Long sessionId,
                                        Long folderId, String storageType, String bucketName,
                                        MultipartFile file) {
        UploadDto dto = new UploadDto();
        dto.setUserId(userId);
        dto.setAnonymousId(anonymousId);
        dto.setSessionId(sessionId);
        dto.setFolderId(folderId);
        // 存储类型默认值：local
        dto.setStorageType(StringUtils.isEmpty(storageType) ? "local" : storageType);
        dto.setBucketName(bucketName);
        dto.setFile(file);
        return dto;
    }
}