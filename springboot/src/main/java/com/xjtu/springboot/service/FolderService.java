package com.xjtu.springboot.service;

import com.xjtu.springboot.dto.file.ChatFileDto;
import com.xjtu.springboot.dto.file.FolderInitDTO;
import com.xjtu.springboot.exception.CustomException;
import com.xjtu.springboot.mapper.FileMapper;
import com.xjtu.springboot.mapper.FolderMapper;
import com.xjtu.springboot.pojo.File;
import com.xjtu.springboot.pojo.Folder;
import com.xjtu.springboot.util.DateUtil;
import com.xjtu.springboot.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderMapper folderMapper;
    private final FileMapper fileMapper;
    private final FileService fileService;

    public Folder getOrInitFolder(ChatFileDto chatFileDto) {
        Long userId = chatFileDto.getUserId();
        Long sessionId = chatFileDto.getSessionId();
        String anonymousId = chatFileDto.getAnonymousId();
        Folder folderData;
        Folder folder = folderMapper.selectByIds(userId, anonymousId, sessionId);
        if (folder != null) {
            folderData = FileUtil.generateFolderData(folder);
            if (folderMapper.updateByPrimaryKey(folderData) == 0) {
                throw new CustomException(500, "更新文件夹数据异常");
            }
        } else {
            folderData = FileUtil.generateFolderData(chatFileDto);
            if (folderMapper.insert(folderData) == 0) {
                throw new CustomException(500, "创建文件夹异常");
            }
        }
        return folderData;
    }

    /**
     * 初始化文件夹上传（创建根文件夹记录）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long initFolderUpload(FolderInitDTO dto) {
        // 参数校验
        if (StringUtils.isEmpty(dto.getFolderUploadId()) || StringUtils.isEmpty(dto.getFolderName())) {
            log.error("文件夹初始化参数异常：folderUploadId={}, folderName={}", dto.getFolderUploadId(), dto.getFolderName());
            throw new IllegalArgumentException("文件夹上传ID和名称不能为空");
        }

        // 检查是否已存在同uploadId的文件夹
        Folder existFolder = folderMapper.selectByFolderUploadId(dto.getFolderUploadId());
        if (existFolder != null) {
            log.warn("文件夹上传ID已存在：{}，直接返回已有ID", dto.getFolderUploadId());
            return existFolder.getId();
        }

        Folder folder = new Folder();
        folder.setUserId(dto.getUserId());
        folder.setAnonymousId(dto.getAnonymousId());
        folder.setSessionId(dto.getSessionId());
        folder.setName(dto.getFolderName());
        folder.setParentId(null); // 根文件夹
        folder.setUploadId(dto.getFolderUploadId());
        folder.setUploadStatus((short) 0); // 0-上传中
        folder.setTotalFiles(dto.getTotalFiles() <= 0 ? 0 : dto.getTotalFiles());
        folder.setUploadedFiles(0);
        folder.setCreatedAt(DateUtil.now()); // 替换原生Date，使用统一日期工具类
        folderMapper.insert(folder);
        log.info("文件夹上传初始化成功：folderId={}, uploadId={}", folder.getId(), dto.getFolderUploadId());
        return folder.getId();
    }

    /**
     * 根据路径递归创建文件夹（支持多级目录）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createFolderByPath(String folderPath, Long userId, String anonymousId, Long sessionId, String folderUploadId) {
        if (StringUtils.isEmpty(folderPath)) {
            log.debug("文件夹路径为空，返回根目录（null）");
            return null; // 根目录
        }

        String[] pathSegments = folderPath.split("/");
        Long parentId = null;

        for (String segment : pathSegments) {
            if (StringUtils.isEmpty(segment) || segment.trim().isEmpty()) {
                continue; // 跳过空段（如路径开头/结尾的/）
            }
            String segmentName = segment.trim();

            // 检查当前层级目录是否已存在
            Folder existFolder = folderMapper.selectByPath(segmentName, parentId, userId, anonymousId, sessionId);
            if (existFolder != null) {
                parentId = existFolder.getId();
                log.debug("目录已存在：{}，parentId={}", segmentName, parentId);
                continue;
            }

            // 创建新目录
            Folder newFolder = new Folder();
            newFolder.setName(segmentName);
            newFolder.setParentId(parentId);
            newFolder.setUserId(userId);
            newFolder.setAnonymousId(anonymousId);
            newFolder.setSessionId(sessionId);
            newFolder.setUploadId(folderUploadId);
            newFolder.setUploadStatus((short) 0); // 上传中
            newFolder.setTotalFiles(0);
            newFolder.setUploadedFiles(0);
            newFolder.setCreatedAt(DateUtil.now());
            folderMapper.insert(newFolder);

            parentId = newFolder.getId();
            log.debug("创建新目录：{}，folderId={}", segmentName, parentId);
        }
        log.info("递归创建文件夹完成：path={}，最终folderId={}", folderPath, parentId);
        return parentId;
    }

    /**
     * 更新文件夹上传进度（累加已上传文件数）
     * @param folderUploadId 文件夹上传ID
     * @param addFiles 新增已上传文件数（单次上传成功的文件数）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateFolderProgress(String folderUploadId, Integer addFiles) {
        if (StringUtils.isEmpty(folderUploadId) || addFiles <= 0) {
            log.warn("文件夹进度更新参数异常：uploadId={}, addFiles={}", folderUploadId, addFiles);
            return false;
        }

        Folder folder = folderMapper.selectByFolderUploadId(folderUploadId);
        if (folder == null) {
            log.error("文件夹上传记录不存在：{}", folderUploadId);
            return false;
        }

        // 累加已上传文件数（核心修正：原逻辑是直接赋值，改为累加）
        int newUploaded = folder.getUploadedFiles() + addFiles;
        folder.setUploadedFiles(Math.min(newUploaded, folder.getTotalFiles())); // 防止超界

        // 所有文件上传完成，更新状态为"上传完成"
        if (folder.getUploadedFiles() >= folder.getTotalFiles() && folder.getTotalFiles() > 0) {
            folder.setUploadStatus((short) 1); // 1-上传完成
            log.info("文件夹上传完成：uploadId={}，总文件数={}，已上传={}",
                    folderUploadId, folder.getTotalFiles(), folder.getUploadedFiles());
        }
        folderMapper.updateByPrimaryKey(folder);
        return true;
    }

    /**
     * 完成文件夹上传（强制更新状态为完成，处理异常场景）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean completeFolderUpload(String folderUploadId) {
        if (StringUtils.isEmpty(folderUploadId)) {
            log.error("文件夹上传ID为空，无法完成上传");
            return false;
        }

        Folder folder = folderMapper.selectByFolderUploadId(folderUploadId);
        if (folder == null) {
            log.error("文件夹上传记录不存在：{}", folderUploadId);
            return false;
        }

        // 更新根文件夹状态为完成
        folder.setUploadStatus((short) 1);
        folderMapper.updateByPrimaryKey(folder);

        // 递归更新所有子文件夹状态为完成
        List<Long> subFolderIds = folderMapper.selectSubFolderIds(folder.getId());
        if (!subFolderIds.isEmpty()) {
            folderMapper.batchUpdateUploadStatus(subFolderIds, (short) 1);
            log.info("批量更新子文件夹状态为完成：folderId={}，子文件夹数={}", folder.getId(), subFolderIds.size());
        }

        log.info("文件夹上传强制完成：uploadId={}", folderUploadId);
        return true;
    }

    /**
     * 删除文件夹（递归删除子文件夹+文件）
     */
    @Transactional(rollbackFor = Exception.class)
    public FileResponseDto deleteFolder(Long folderId, Long userId, String anonymousId) {
        // 1. 校验文件夹存在性
        Folder folder = folderMapper.selectByPrimaryKey(folderId);
        if (folder == null) {
            log.warn("删除文件夹失败：文件夹不存在，folderId={}", folderId);
            return FileResponseDto.fail("文件夹不存在");
        }

        // 2. 权限校验
        boolean hasPermission = (userId != null && userId.equals(folder.getUserId()))
                || (StringUtils.isNotEmpty(anonymousId) && anonymousId.equals(folder.getAnonymousId()));
        if (!hasPermission) {
            log.warn("删除文件夹权限不足：folderId={}，userId={}，anonymousId={}",
                    folderId, userId, anonymousId);
            return FileResponseDto.fail("无权限删除该文件夹");
        }

        try {
            // 3. 递归删除所有子文件夹
            List<Long> subFolderIds = folderMapper.selectSubFolderIds(folderId);
            if (!subFolderIds.isEmpty()) {
                for (Long subFolderId : subFolderIds) {
                    deleteFolder(subFolderId, userId, anonymousId); // 递归删除子文件夹
                }
            }

            // 4. 删除当前文件夹下所有文件
            List<File> fileList = fileMapper.selectByFolderId(folderId);
            if (!fileList.isEmpty()) {
                for (File file : fileList) {
                    fileService.deleteFile(file.getId(), userId, anonymousId); // 调用FileService删除文件
                }
                log.info("删除文件夹下文件完成：folderId={}，文件数={}", folderId, fileList.size());
            }

            // 5. 删除当前文件夹
            folderMapper.deleteByPrimaryKey(folderId);
            log.info("删除文件夹成功：folderId={}，名称={}", folderId, folder.getName());
            return FileResponseDto.success().setMessage("文件夹删除成功");

        } catch (Exception e) {
            log.error("删除文件夹失败：folderId={}", folderId, e);
            return FileResponseDto.fail("文件夹删除失败：" + e.getMessage());
        }
    }

    /**
     * 扩展：根据ID查询文件夹（供外部调用）
     */
    public Folder getFolderById(Long folderId) {
        if (folderId == null || folderId <= 0) {
            return null;
        }
        return folderMapper.selectByPrimaryKey(folderId);
    }

    /**
     * 扩展：批量更新文件夹状态（供内部调用）
     */
    public int batchUpdateSubFolderStatus(Long parentId, Short status) {
        if (parentId == null || status == null) {
            return 0;
        }
        List<Long> subFolderIds = folderMapper.selectSubFolderIds(parentId);
        if (subFolderIds.isEmpty()) {
            return 0;
        }
        return folderMapper.batchUpdateUploadStatus(subFolderIds, status);
    }
}