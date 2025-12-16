package com.xjtu.springboot.service;

import cn.hutool.core.io.FileUtil;
import com.xjtu.springboot.controller.ChatController;
import com.xjtu.springboot.exception.CustomException;
import com.xjtu.springboot.mapper.FileFolderMapper;
import com.xjtu.springboot.mapper.FileMapper;
import com.xjtu.springboot.pojo.File;
import com.xjtu.springboot.pojo.FileFolder;
import com.xjtu.springboot.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class FileService {
    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private FileFolderMapper fileFolderMapper;

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private static final String FILE_IDS_SEPARATOR = ",";
    private static final String FILE_PATH_PREFIX = "/files/";
    private static final String FILE_PATH = System.getProperty("user.dir") + FILE_PATH_PREFIX;
    private static final String LOCALHOST = "http://localhost:8090";

    public File uploadFile(MultipartFile multipartFile, File file) {
        File localFile = new File();
        try {
            if (!FileUtil.isDirectory(FILE_PATH)) {
                FileUtil.mkdir(FILE_PATH);
            }

            String originalFilename = multipartFile.getOriginalFilename();
            String fileName = System.currentTimeMillis() + "_" + originalFilename;
            String realFileName = FILE_PATH + fileName;
            FileUtil.writeBytes(multipartFile.getBytes(), realFileName);

            localFile.copyFrom(file);
            localFile.setFileName(fileName);
            // TODO 暂时放在本地文件夹
            localFile.setFilePath(LOCALHOST + FILE_PATH_PREFIX + fileName);
            localFile.setUploadTime(DateUtil.now());

            // 上传文件
            uploadFile(localFile);
        } catch (Exception e) {
            log.error("上传文件异常：{}", e.getMessage(), e);
        }

        return localFile;
    }

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
                throw new CustomException(500, "创建文件逻辑根目录异常");
            }
        }

        file.setFolderId(fileFolder.getId());
        if (fileMapper.insert(file) <= 0) {
            throw new CustomException(500, "添加文件信息异常");
        }
    }

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

    public byte[] downloadFile(String fileName) {
        String filePath = getFilePath(fileName);
        return FileUtil.readBytes(filePath);
    }

    public String getFilePath(String fileName) {
        return FILE_PATH + fileName;
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
