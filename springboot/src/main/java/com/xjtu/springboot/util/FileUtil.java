package com.xjtu.springboot.util;

import com.xjtu.springboot.dto.file.ChatFileDto;
import com.xjtu.springboot.pojo.File;
import com.xjtu.springboot.pojo.FileContent;
import com.xjtu.springboot.pojo.Folder;
import com.xjtu.springboot.pojo.common.Unit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

@Slf4j
public class FileUtil {
    // 登录返回true
    public static Boolean checkParams(Long userId, String anonymousId) {
        if (StringUtils.isNotEmpty(anonymousId)) {
            return false;
        }
        return userId != null && userId > 0;
    }

    // 登录返回true
    public static Boolean checkParams(Long userId, String anonymousId, Long sessionId) {
        if (StringUtils.isNotEmpty(anonymousId)) {
            return false;
        }
        return userId != null && userId > 0 && sessionId != null && sessionId > 0;
    }

    public static Long formatFileSize(Long size, Unit unit) {
        return size / unit.getStep();
    }

    /**
     * 计算MultipartFile的MD5
     */
    public static String calculateMd5(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            return calculateMd5(is);
        } catch (IOException e) {
            log.error("计算文件MD5失败", e);
            return null;
        }
    }

    /**
     * 计算输入流的MD5
     */
    public static String calculateMd5(InputStream is) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                md5.update(buffer, 0, len);
            }
            byte[] digest = md5.digest();
            // 转16进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("计算MD5失败", e);
            return null;
        }
    }

    public static String getStorageType(String storage) {
        String res = "local";
        if (StringUtils.isNotEmpty(storage)) {
            res = switch (storage) {
                case "local" -> "local";
                case "oss" -> "oss";
                default -> "";
            };
        }
        return res;
    }

    // 获取文件经过处理之后的文件名
    public static String getCleanedName(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName != null) {
            int lastDotIndex = originalName.lastIndexOf(".");
            String namePart = originalName.substring(0, lastDotIndex);
            String extPart = originalName.substring(lastDotIndex);
            return namePart.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]+", "_") + extPart;
        }

        return "";
    }

    // 获取文件扩展名
    public static String getExtension(MultipartFile file) {
        Objects.requireNonNull(file, "解析文件不能为空");
        String fileName = Objects.requireNonNull(file.getOriginalFilename(), "文件名不能为空");
        return FilenameUtils.getExtension(fileName).toLowerCase(Locale.ROOT);
    }

    public static boolean isImage(MultipartFile file) {
        return FileUtil.isImageByContentType(file) && FileUtil.isImageByMagicNumber(file);
    }

    /**
     * 简单判断是否是图片
     */
    private static boolean isImageByContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * 精准判断是否为图片（文件头魔数方式）
     */
    private static boolean isImageByMagicNumber(MultipartFile file) {
        if (file.isEmpty()) {
            return false;
        }
        // 读取文件前几个字节（魔数长度最多8位）
        try (InputStream is = file.getInputStream()) {
            byte[] magicBytes = new byte[8];
            int read = is.read(magicBytes);
            if (read < 4) { // 至少读取4位才能判断
                return false;
            }
            // 匹配常见图片魔数
            String magicHex = CommonUtil.bytesToHex(magicBytes).toUpperCase();
            return magicHex.startsWith("FFD8FF") // JPG/JPEG
                    || magicHex.startsWith("89504E47") // PNG
                    || magicHex.startsWith("47494638") // GIF
                    || magicHex.startsWith("424D"); // BMP
        } catch (IOException e) {
            return false;
        }
    }

    // 获取文件过期时间，如果登录了就不过期，否则24小时后过期
    public static LocalDateTime getExpireAt(Long userId, String anonymousId) {
        if (checkParams(userId, anonymousId)) {
            return null;
        }
        return DateUtil.plusTime(24, ChronoUnit.HOURS);
    }

    public static File generateFileData(MultipartFile file, ChatFileDto chatFileDto,
                                        HashMap<String, Object> data) {
        File fileData = new File();
        fileData.setUserId(chatFileDto.getUserId());
        fileData.setSessionId(chatFileDto.getSessionId());
        fileData.setAnonymousId(chatFileDto.getAnonymousId());
        fileData.setFolderId((Long) data.get("folderId"));
        fileData.setNewName(data.get("newFileName").toString());
        fileData.setOriginalName(file.getOriginalFilename());
        fileData.setSize(file.getSize());
        fileData.setStoragePath(data.get("storagePath").toString());
        fileData.setAccessUrl(data.get("accessUrl").toString());
        int[] imageInfo = ImageUtil.checkAndGetSize(file);
        fileData.setIsImage((short) imageInfo[0]);
        fileData.setImageWidth(imageInfo[1]);
        fileData.setImageHeight(imageInfo[2]);
        LocalDateTime now = DateUtil.now();
        fileData.setUploadAt(now);
        fileData.setFileMd5(data.get("fileMd5").toString());
        fileData.setStorageType(data.get("storageType").toString());
        fileData.setExpireAt(getExpireAt(chatFileDto.getUserId(), chatFileDto.getAnonymousId()));
        // TODO
        fileData.setRelativePath("");
        fileData.setIsDeleted((short) 0);
        fileData.setUpdatedAt(now);
        return fileData;
    }

    public static FileContent generateFileContent(Long fileId, String content, String ext) {
        FileContent fileContent = new FileContent();
        fileContent.setFileId(fileId);
        fileContent.setType(ext);
        fileContent.setContent(content);
        LocalDateTime now = DateUtil.now();
        fileContent.setCreatedAt(now);
        fileContent.setIsDeleted((short) 0);
        fileContent.setUpdatedAt(now);
        return fileContent;
    }

    public static Folder generateFolderData(ChatFileDto chatFileDto) {
        Folder folderData = new Folder();
        folderData.setUserId(chatFileDto.getUserId());
        folderData.setAnonymousId(chatFileDto.getAnonymousId());
        folderData.setParentId(null);
        folderData.setCreatedAt(DateUtil.now());
        folderData.setUploadId(CommonUtil.getUUID());
        folderData.setTotalFiles(1);
        folderData.setUploadedFiles(1);
        folderData.setIsDeleted((short) 0);
        folderData.setUpdatedAt(DateUtil.now());
        return folderData;
    }

    public static Folder generateFolderData(Folder folder) {
        Folder folderData = new Folder();
        folderData.setId(folder.getId());
        folderData.setUserId(folder.getUserId());
        folderData.setAnonymousId(folder.getAnonymousId());
        folderData.setParentId(folder.getParentId());
        folderData.setCreatedAt(folder.getCreatedAt());
        folderData.setUploadId(folder.getUploadId());
        folderData.setTotalFiles(folder.getTotalFiles() + 1);
        folderData.setUploadedFiles(folder.getUploadedFiles() + 1);
        folderData.setIsDeleted((short) 0);
        folderData.setUpdatedAt(DateUtil.now());
        return folderData;
    }

    public static ChatFileDto generateChatFileDto(ChatFileDto chatFileDto, File file, Folder folder) {
        ChatFileDto result = new ChatFileDto();
        result.setUserId(chatFileDto.getUserId());
        result.setSessionId(chatFileDto.getSessionId());
        result.setAnonymousId(chatFileDto.getAnonymousId());
        result.setFolderId(folder.getId());
        result.setFileMd5(file.getFileMd5());
        result.setStorageType(chatFileDto.getStorageType());
        result.setSucceed(true);
        return result;
    }
}
