package com.xjtu.springboot.dto.file;

import com.xjtu.springboot.pojo.File;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.Date;

@Data
public class FileVO {
    private Long id;
    private Long userId;
    private String anonymId;
    private Long sessionId;
    private Long folderId;
    private String folderName; // 文件夹名称
    private String newName;
    private String originalName;
    private String type;
    private Long size;
    private String sizeStr; // 格式化后的文件大小（如10MB）
    private String storagePath;
    private String accessUrl;
    private Integer isImage;
    private Integer imageWidth;
    private Integer imageHeight;
    private Date uploadAt;
    private String storageType;
    private String mimeType;
    private Integer isCompressed;
    private String relativePath;

    // 转换方法
    public static FileVO from(File filePO, String folderName) {
        FileVO vo = new FileVO();
        BeanUtils.copyProperties(filePO, vo);
        vo.setFolderName(folderName);
        // 格式化文件大小
        vo.setSizeStr(formatFileSize(filePO.getSize()));
        return vo;
    }

    // 格式化文件大小
    private static String formatFileSize(Long size) {
        if (size < 1024) {
            return size + "B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2fKB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2fMB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2fGB", size / (1024.0 * 1024 * 1024));
        }
    }
}