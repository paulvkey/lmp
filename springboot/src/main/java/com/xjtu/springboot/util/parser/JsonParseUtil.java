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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    public static String parse(MultipartFile file) {
        // 参数校验
        if (file == null || file.isEmpty()) {
            return "【错误】上传的JSON文件为空";
        }

        // 校验文件格式
        String fileName = Objects.requireNonNull(file.getOriginalFilename(), "文件名不能为空");
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        if (!SUPPORTED_FORMATS.contains(suffix)) {
            return "【错误】不支持的文件格式（仅支持json），当前文件：" + fileName;
        }

        // 解析JSON并格式化
        try {
            // 读取文件内容
            String jsonContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            // 校验JSON语法合法性
            JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonContent);
            // 格式化JSON
            String formattedJson = OBJECT_MAPPER.writeValueAsString(jsonNode);

            // 拼接结构化内容
            String ragContent = "=== JSON文档元信息 ===\n" +
                    "文件名：" + fileName + "\n" +
                    "文件大小：" + file.getSize() + "字节\n" +
                    "字符编码：UTF-8\n\n" +
                    "=== 格式化JSON内容 ===\n" +
                    formattedJson;

            return ragContent.trim();
        } catch (IOException e) {
            return "【错误】JSON解析失败（语法错误/格式异常）：" + e.getMessage();
        }
    }
}
