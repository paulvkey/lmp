package com.xjtu.springboot.util;

import com.xjtu.springboot.util.parser.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;


public class FileParseUtil {
    // ========== 静态工具初始化 ==========
    private static final Tika TIKA = new Tika();

    /**
     * 统一解析入口
     */
    public static String parse(MultipartFile file) throws Exception {
        Objects.requireNonNull(file, "解析文件不能为空");
        String fileName = Objects.requireNonNull(file.getOriginalFilename(), "文件名不能为空");
        String ext = FilenameUtils.getExtension(fileName).toLowerCase(Locale.ROOT);

        // 简化switch，避免Java 17语法兼容问题
        if ("pdf".equals(ext)) {
            return PdfParseUtil.extractPdfForRAG(file);
        } else if ("doc".equals(ext) || "docx".equals(ext)) {
            return DocParseUtil.extractWordForRAG(file);
        } else if ("xls".equals(ext) || "xlsx".equals(ext)) {
            return ExcelParseUtil.extractExcelForRAG(file);
        } else if ("md".equals(ext) || "markdown".equals(ext)) {
            return MdParseUtil.parseMarkdownForRAG(file);
        } else if ("json".equals(ext)) {
            return JsonParseUtil.extractJsonForRAG(file);
        } else if ("xml".equals(ext)) {
            return XmlParseUtil.extractXmlForRAG(file);
        } else {
            return parseWithTika(file);
        }
    }

    // ========== Tika兜底解析 ==========
    private static String parseWithTika(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream()) {
            return "=== 通用解析内容 ===\n" + TIKA.parseToString(is);
        }
    }

}