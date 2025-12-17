package com.xjtu.springboot.service;

import cn.hutool.core.io.FileUtil;
import com.xjtu.springboot.controller.ChatController;
import com.xjtu.springboot.exception.CustomException;
import com.xjtu.springboot.mapper.FileFolderMapper;
import com.xjtu.springboot.mapper.FileMapper;
import com.xjtu.springboot.pojo.File;
import com.xjtu.springboot.pojo.FileFolder;
import com.xjtu.springboot.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class FileService {
    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private FileFolderMapper fileFolderMapper;

    private static final String FILE_IDS_SEPARATOR = ",";
    private static final String FILE_PATH_PREFIX = "/files/";
    private static final String FILE_PATH = System.getProperty("user.dir") + FILE_PATH_PREFIX;
    private static final String LOCALHOST = "http://localhost:8090";

    @Transactional
    public File uploadFile(MultipartFile multipartFile, File file) {
        File localFile = new File();
        try {
            Long userId = file.getUserId();
            Long sessionId = file.getSessionId();
            // TODO 暂时放在本地文件夹
            String userPath = getFilePath(userId, sessionId, "");
            if (!FileUtil.isDirectory(userPath)) {
                FileUtil.mkdir(userPath);
            }

            String fileName = multipartFile.getOriginalFilename();
            String realFileName = userPath + fileName;
            FileUtil.writeBytes(multipartFile.getBytes(), realFileName);

            localFile.copyFrom(file);
            localFile.setFileName(fileName);
            localFile.setFilePath(realFileName);
            localFile.setUploadTime(DateUtil.now());

            // 上传文件
            uploadFile(localFile);
        } catch (Exception e) {
            log.error("上传文件异常：{}", e.getMessage(), e);
            throw new CustomException(500, e.getMessage());
        }

        return localFile;
    }

    @Transactional
    public void uploadFile(File file) {
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
                log.error("创建文件逻辑根目录异常: userId={}, sessionId={}", userId, sessionId);
                throw new CustomException(500, "创建文件逻辑根目录异常");
            }
        }

        file.setFolderId(fileFolder.getId());
        if (fileMapper.insert(file) <= 0) {
            log.error("添加文件信息异常: userId={}, sessionId={}", userId, sessionId);
            throw new CustomException(500, "添加文件信息异常");
        }
    }

    @Transactional
    public List<File> uploadFiles(List<MultipartFile> multipartFileList, List<File> fileList) {
        List<File> files = new ArrayList<>();
        for (int i = 0; i < fileList.size(); i++) {
            files.add(uploadFile(multipartFileList.get(i), fileList.get(i)));
        }
        return files;
    }

    public String generateFileIds(List<File> files) {
        if (files == null || files.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(files.get(0).getId());
        for (int i = 1; i < files.size(); i++) {
            sb.append(FILE_IDS_SEPARATOR).append(files.get(i).getId());
        }
        return sb.toString();
    }

    public byte[] downloadFile(Long userId, Long sessionId, String fileName) {
        String filePath = getFilePath(userId, sessionId, fileName);
        return FileUtil.readBytes(filePath);
    }

    public String getFilePath(Long userId, Long sessionId, String fileName) {
        return FILE_PATH + userId + "/" + sessionId + "/" + fileName;
    }

    public String getContentType(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            case "doc", "docx" -> "application/msword";
            case "xls", "xlsx" -> "application/vnd.ms-excel";
            default -> "application/octet-stream"; // 未知类型用二进制流
        };
    }

}
