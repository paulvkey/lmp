package com.xjtu.springboot.util.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;

public class JsonParseUtil {
    // 支持的JSON文件后缀
    private static final Set<String> SUPPORTED_FORMATS = Set.of("json");
    // Jackson ObjectMapper（格式化输出）
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    /**
     * 核心方法：解析JSON文件，返回RAG友好的结构化字符串
     * @param file 上传的JSON文件
     * @return 格式化JSON + 元信息
     */
    public static String extractJsonForRAG(MultipartFile file) {
        // 1. 参数校验
        if (file == null || file.isEmpty()) {
            return "【错误】上传的JSON文件为空";
        }

        // 2. 校验文件格式
        String fileName = Objects.requireNonNull(file.getOriginalFilename(), "文件名不能为空");
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        if (!SUPPORTED_FORMATS.contains(suffix)) {
            return "【错误】不支持的文件格式（仅支持json），当前文件：" + fileName;
        }

        // 3. 解析JSON并格式化
        try {
            // 读取文件内容（UTF-8编码，避免乱码）
            String jsonContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            // 校验JSON语法合法性
            JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonContent);
            // 格式化JSON（4空格缩进）
            String formattedJson = OBJECT_MAPPER.writeValueAsString(jsonNode);

            // 拼接RAG友好的结构化内容
            StringBuilder ragContent = new StringBuilder();
            ragContent.append("=== JSON文档元信息 ===\n");
            ragContent.append("文件名：").append(fileName).append("\n");
            ragContent.append("文件大小：").append(file.getSize()).append("字节\n");
            ragContent.append("字符编码：UTF-8\n\n");
            ragContent.append("=== 格式化JSON内容 ===\n");
            ragContent.append(formattedJson);

            return ragContent.toString().trim();
        } catch (IOException e) {
            return "【错误】JSON解析失败（语法错误/格式异常）：" + e.getMessage();
        }
    }
}
