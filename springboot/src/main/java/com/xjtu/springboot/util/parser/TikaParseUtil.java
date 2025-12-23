package com.xjtu.springboot.util.parser;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Property;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;


public class TikaParseUtil {

    private static final Tika TIKA = new Tika();
    private static final AutoDetectParser PARSER = new AutoDetectParser();
    private static final Pattern NOISE_PATTERN = Pattern.compile("\\s{2,}|\\n{3,}|\\t+");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    // RAG分块大小
    private static final int RAG_CHUNK_SIZE = 500;

    public static String parse(MultipartFile file) {
        // 基础校验
        if (file == null || file.isEmpty()) {
            return "【错误】上传的文件为空";
        }

        String fileName = Optional.ofNullable(file.getOriginalFilename()).orElse("未知文件名");
        String suffix = getFileSuffix(fileName).toLowerCase();
        try (InputStream inputStream = file.getInputStream()) {
            Metadata metadata = new Metadata();
            // 无字符数限制
            ContentHandler contentHandler = new BodyContentHandler(-1);
            ParseContext context = new ParseContext();
            context.set(org.apache.tika.parser.Parser.class, PARSER);

            // Tika解析（自动识别兜底格式）
            PARSER.parse(inputStream, contentHandler, metadata, context);
            // 构建结构化内容
            StringBuilder ragContent = new StringBuilder();
            // 元信息
            ragContent.append("=== 文档元信息 ===\n");
            ragContent.append("文件名：").append(fileName).append("\n");
            ragContent.append("文件大小：").append(file.getSize()).append(" 字节\n");
            ragContent.append("兜底格式：").append(getFallbackFormat(metadata, fileName)).append("\n");
            ragContent.append("MIME类型：").append(Optional.ofNullable(metadata.get(Metadata.CONTENT_TYPE)).orElse("未知")).append("\n");
            ragContent.append("作者：").append(getMetadataValue(metadata, TikaCoreProperties.CREATOR, "未知")).append("\n");
            ragContent.append("创建时间：").append(getMetadataDate(metadata, TikaCoreProperties.CREATED, "未知")).append("\n\n");

            // 清洗正文
            String rawText = contentHandler.toString();
            String cleanText = NOISE_PATTERN.matcher(rawText)
                    .replaceAll(m -> m.group().contains("\n") ? "\n\n" : " ")
                    .trim();

            // 文本分块
            ragContent.append("=== 结构化文本（RAG分块）===\n");
            List<String> chunks = splitTextToRagChunks(cleanText);
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i).trim();
                if (!chunk.isEmpty()) {
                    ragContent.append("【文本块").append(i + 1).append("】\n").append(chunk).append("\n\n");
                }
            }

            // 无正文（如图片需OCR）
            if (chunks.isEmpty() || cleanText.isEmpty()) {
                ragContent.append("【提示】未提取到文本内容（若为图片/扫描件，请开启OCR解析）\n");
            }

            return ragContent.toString().trim();
        } catch (Exception e) {
            return String.format(
                    "【错误】兜底解析失败：%s（文件：%s，格式：%s）",
                    e.getMessage(), fileName, suffix
            );
        }
    }

    // ========== 获取文件后缀 ==========
    private static String getFileSuffix(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        return lastDotIndex == -1 ? "" : fileName.substring(lastDotIndex + 1);
    }

    // ========== 识别兜底格式名称 ==========
    private static String getFallbackFormat(Metadata metadata, String fileName) {
        String mimeType = metadata.get(Metadata.CONTENT_TYPE);
        if (mimeType == null) {
            return getFileSuffix(fileName).toUpperCase();
        }
        if (mimeType.contains("text/plain")) return "TXT";
        if (mimeType.contains("html") || mimeType.contains("xhtml")) return "HTML";
        if (mimeType.contains("powerpoint") || mimeType.contains("pptx")) return "PPT/PPtx";
        if (mimeType.contains("csv")) return "CSV";
        if (mimeType.contains("image")) return "图片（需OCR）";
        return getFileSuffix(fileName).toUpperCase();
    }

    // ========== 提取元信息值（处理Property类型） ==========
    private static String getMetadataValue(Metadata metadata, Property property, String defaultValue) {
        List<String> values = List.of(metadata.getValues(property));
        return values.isEmpty() ? defaultValue : String.join(", ", values);
    }

    // ========== 提取元信息日期 ==========
    private static String getMetadataDate(Metadata metadata, Property property, String defaultValue) {
        Date date = metadata.getDate(property);
        if (date == null) {
            return defaultValue;
        }
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(date);
        }
    }

    // ========== 文本分块 ==========
    private static List<String> splitTextToRagChunks(String cleanText) {
        List<String> chunks = new ArrayList<>();
        if (cleanText.isEmpty()) return chunks;

        String[] paragraphs = cleanText.split("\\n\\n");
        StringBuilder currentChunk = new StringBuilder();

        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) continue;

            if (currentChunk.length() + para.length() > RAG_CHUNK_SIZE) {
                chunks.add(currentChunk.toString());
                currentChunk.setLength(0);
            }
            currentChunk.append(para).append("\n\n");
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString());
        }
        return chunks;
    }
}
