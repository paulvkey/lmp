package com.xjtu.springboot.service;

import com.xjtu.springboot.dto.file.FolderInitDTO;
import com.xjtu.springboot.mapper.FolderMapper;
import com.xjtu.springboot.pojo.Folder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 文件夹服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderMapper folderMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long initFolderUpload(FolderInitDTO dto) {
        Folder folder = new Folder();
        folder.setUserId(dto.getUserId());
        folder.setAnonymId(dto.getAnonymId());
        folder.setSessionId(dto.getSessionId());
        folder.setName(dto.getFolderName());
        folder.setParentId(null); // 根文件夹
        folder.setUploadId(dto.getFolderUploadId());
        folder.setUploadStatus((short) 0); // 上传中
        folder.setTotalFiles(dto.getTotalFiles());
        folder.setUploadedFiles(0);
        folder.setCreatedAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
        folderMapper.insert(folder);
        return folder.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createFolderByPath(String folderPath, Long userId, String anonymId, Long sessionId, String folderUploadId) {
        if (StringUtils.isEmpty(folderPath)) {
            return null; // 根目录
        }

        String[] pathSegments = folderPath.split("/");
        Long parentId = null;

        for (String segment : pathSegments) {
            if (StringUtils.isEmpty(segment)) {
                continue;
            }
            // 检查目录是否已存在
            Folder existFolder = folderMapper.selectByPath(segment, parentId, userId, anonymId, sessionId);
            if (existFolder != null) {
                parentId = existFolder.getId();
                continue;
            }

            // 创建新目录
            Folder newFolder = new Folder();
            newFolder.setName(segment);
            newFolder.setParentId(parentId);
            newFolder.setUserId(userId);
            newFolder.setAnonymId(anonymId);
            newFolder.setSessionId(sessionId);
            newFolder.setUploadId(folderUploadId);
            newFolder.setUploadStatus((short) 0);
            newFolder.setTotalFiles(0);
            newFolder.setUploadedFiles(0);
            newFolder.setCreatedAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
            folderMapper.insert(newFolder);

            parentId = newFolder.getId();
        }
        return parentId;
    }

    public boolean updateFolderProgress(String folderUploadId, Integer uploadedFiles) {
        Folder folder = folderMapper.selectByFolderUploadId(folderUploadId);
        if (folder == null) {
            log.error("文件夹上传记录不存在：{}", folderUploadId);
            return false;
        }
        folder.setUploadedFiles(uploadedFiles);
        // 所有文件上传完成
        if (folder.getUploadedFiles() >= folder.getTotalFiles()) {
            folder.setUploadStatus((short) 1); // 上传完成
        }
        folderMapper.updateByPrimaryKey(folder);
        return true;
    }

    public boolean completeFolderUpload(String folderUploadId) {
        Folder folder = folderMapper.selectByFolderUploadId(folderUploadId);
        if (folder == null) {
            log.error("文件夹上传记录不存在：{}", folderUploadId);
            return false;
        }
        folder.setUploadStatus((short) 1); // 上传完成
        folderMapper.updateByPrimaryKey(folder);
        return true;
    }

    public UploadResponseDTO deleteFolder(Long folderId, Long userId, String anonymId) {
        // 1. 校验文件夹及权限
        Folder folder = folderMapper.selectByPrimaryKey(folderId);
        if (folder == null) {
            return UploadResponseDTO.fail("文件夹不存在");
        }
        boolean hasPermission = (userId != null && userId.equals(folder.getUserId()))
                || (StringUtils.isNotEmpty(anonymId) && anonymId.equals(folder.getAnonymId()));
        if (!hasPermission) {
            return UploadResponseDTO.fail("无权限删除该文件夹");
        }

        // 2. 递归删除所有子文件夹
        List<Long> subFolderIds = folderMapper.selectSubFolderIds(folderId);
        for (Long subFolderId : subFolderIds) {
            deleteFolder(subFolderId, userId, anonymId);
        }

        // 3. 删除文件夹下所有文件
        List<File> fileList = fileMapper.selectByFolderId(folderId);
        for (File file : fileList) {
            deleteFile(file.getId(), userId, anonymId);
        }

        // 4. 删除当前文件夹
        folderMapper.deleteByPrimaryKey(folderId);

        return UploadResponseDTO.success();
    }
}