package com.xjtu.springboot.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;

@Slf4j
public class FileUtil {
    public static Boolean checkParams(Long userId, String anonymousId) {
        if (StringUtils.isEmpty(anonymousId)) {
            return userId != null && userId > 0;
        }
        return true;
    }

    public static Boolean checkParams(Long userId, String anonymousId, Long sessionId) {
        if (StringUtils.isEmpty(anonymousId)) {
            return userId != null && userId > 0 && sessionId != null && sessionId > 0;
        }
        return true;
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
     * 计算文件的MD5
     */
    public static String calculateMd5(File file) {
        try (InputStream is = new FileInputStream(file)) {
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


}
