package com.xjtu.springboot.controller;

import com.xjtu.springboot.common.Result;
import com.xjtu.springboot.dto.file.BreakpointCheckDTO;
import com.xjtu.springboot.dto.file.ChunkUploadDTO;
import com.xjtu.springboot.dto.file.UploadDto;
import com.xjtu.springboot.dto.file.FolderInitDTO;
import com.xjtu.springboot.dto.file.UploadResponseDTO;
import com.xjtu.springboot.service.FileService;
import com.xjtu.springboot.service.FolderService;
import com.xjtu.springboot.util.FileUtil;
import com.xjtu.springboot.util.CommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Slf4j
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FolderService folderService;


    @PostMapping("/single/upload")
    public Result uploadSingleFile(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String anonymId,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) String storageType,
            @RequestParam(required = false) String bucketName,
            @RequestParam("file") MultipartFile file) {
        if (FileUtil.checkParams(userId, anonymId)) {
            try {
                UploadDto dto = generateUploadDto(userId, anonymId, sessionId, folderId, storageType, bucketName, file);
                UploadResponseDTO uploadResponseDTO = fileService.uploadSingleFile(dto);
                if (Objects.nonNull(uploadResponseDTO)) {
                    return Result.success(uploadResponseDTO);
                }
            } catch (Exception e) {
                return Result.error(e.getMessage());
            }
        }

        return Result.error("");
    }

    /**
     * 多个文件上传
     */
    @PostMapping("/multiple/upload")
    public UploadResponseDTO uploadMultipleFiles(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String anonymId,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) String storageType,
            @RequestParam(required = false) String bucketName,
            @RequestParam("files") MultipartFile[] files) {

        List<UploadDto> dtoList = new ArrayList<>();
        for (MultipartFile file : files) {
            UploadDto dto = generateUploadDto(userId, anonymId, sessionId, folderId, storageType, bucketName, file);
            dtoList.add(dto);
        }
        return fileService.uploadMultipleFiles(dtoList);
    }

    /**
     * 断点续传校验
     */
    @PostMapping("/breakpoint/check")
    public UploadResponseDTO checkBreakpoint(@Valid @RequestBody BreakpointCheckDTO dto) {
        return fileService.checkBreakpoint(dto);
    }

    /**
     * 分片上传
     */
    @PostMapping("/chunk/upload")
    public UploadResponseDTO uploadChunk(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String anonymId,
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

        ChunkUploadDTO dto = new ChunkUploadDTO();
        dto.setUserId(userId);
        dto.setAnonymId(anonymId);
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
        return fileService.uploadChunk(dto);
    }

    /**
     * 合并分片
     */
    @PostMapping("/chunk/merge")
    public UploadResponseDTO mergeChunks(
            @RequestParam("uploadId") String uploadId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String anonymId,
            @RequestParam(required = false) Long sessionId,
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam("originalFileName") String originalFileName) {

        return fileService.mergeChunks(uploadId, userId, anonymId, sessionId, fileMd5, originalFileName);
    }

    /**
     * 初始化文件夹上传
     */
    @PostMapping("/folder/init")
    public UploadResponseDTO initFolderUpload(@Valid @RequestBody FolderInitDTO dto) {
        Long folderId = folderService.initFolderUpload(dto);
        UploadResponseDTO response = UploadResponseDTO.success();
        response.setFolderId(folderId);
        return response;
    }

    /**
     * 目录上传（文件夹上传）
     */
    @PostMapping("/folder/upload")
    public UploadResponseDTO uploadFolder(
            @RequestParam("folderUploadId") String folderUploadId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String anonymId,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) String storageType,
            @RequestParam(required = false) String bucketName,
            @RequestParam("files") MultipartFile[] files) {

        List<UploadDto> dtoList = new ArrayList<>();
        for (MultipartFile file : files) {
            // 获取文件相对路径（前端webkitdirectory返回）
            String relativePath = file.getOriginalFilename();
            UploadDto dto = new UploadDto();
            dto.setUserId(userId);
            dto.setAnonymId(anonymId);
            dto.setSessionId(sessionId);
            dto.setStorageType(storageType);
            dto.setBucketName(bucketName);
            dto.setFile(file);
            dto.setRelativePath(relativePath);
            dtoList.add(dto);
        }
        return fileService.uploadFolder(folderUploadId, dtoList, userId, anonymId, sessionId);
    }

    /**
     * 解析文件内容
     */
    @GetMapping("/content/parse")
    public UploadResponseDTO parseFileContent(@RequestParam("fileId") Long fileId) {
        String content = fileService.parseFileContent(fileId);
        UploadResponseDTO response = UploadResponseDTO.success();
        if (content == null) {
            response.setCode(500);
            response.setMessage("解析文件内容失败");
        } else {
            response.setMessage(content);
        }
        return response;
    }

    /**
     * 生成上传ID（前端调用）
     */
    @GetMapping("/uploadId/generate")
    public UploadResponseDTO generateUploadId() {
        UploadResponseDTO response = UploadResponseDTO.success();
        response.setMessage(CommonUtil.generateUUID());
        return response;
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(
            @RequestParam Long fileId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String anonymId,
            HttpServletRequest request,
            HttpServletResponse response) {
        return fileService.downloadFile(fileId, userId, anonymId, request, response);
    }

    /**
     * 单个文件删除
     */
    @DeleteMapping("/delete")
    public UploadResponseDTO deleteFile(
            @RequestParam Long fileId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String anonymId) {
        return fileService.deleteFile(fileId, userId, anonymId);
    }

    /**
     * 文件夹级联删除（删除文件夹及所有子文件/子文件夹）
     */
    @DeleteMapping("/folder/delete")
    public UploadResponseDTO deleteFolder(
            @RequestParam Long folderId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String anonymId) {
        return folderService.deleteFolder(folderId, userId, anonymId);
    }

    /**
     * 文件列表查询（支持多条件分页）
     */
    @PostMapping("/list")
    public PageInfo<FileVO> queryFileList(@RequestBody FileQueryDTO dto) {
        return fileService.queryFileList(dto);
    }

    public UploadDto generateUploadDto(Long userId, String anonymId, Long sessionId,
                                       Long folderId, String storageType, String bucketName,
                                       MultipartFile file) {
        UploadDto dto = new UploadDto();
        dto.setUserId(userId);
        dto.setAnonymId(anonymId);
        dto.setSessionId(sessionId);
        dto.setFolderId(folderId);
        dto.setStorageType(storageType);
        dto.setBucketName(bucketName);
        dto.setFile(file);
        return dto;
    }
}