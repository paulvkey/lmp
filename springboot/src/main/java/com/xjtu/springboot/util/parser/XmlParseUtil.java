package com.xjtu.springboot.util.parser;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;

public class XmlParseUtil {
    // 支持的XML文件后缀
    private static final Set<String> SUPPORTED_FORMATS = Set.of("xml");

    // 全局XmlMapper
    private static final XmlMapper XML_MAPPER;

    // 静态初始化：配置XML格式化（统一使用SerializationFeature）
    static {
        XML_MAPPER = new XmlMapper();
        // 启用缩进
        XML_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        // 禁用空Bean失败
        XML_MAPPER.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    /**
     * 解析XML文件，返回RAG友好的结构化字符串
     */
    public static String extractXmlForRAG(MultipartFile file) {
        // 1. 参数校验
        if (file == null || file.isEmpty()) {
            return "【错误】上传的XML文件为空";
        }

        // 2. 校验文件格式
        String fileName = Objects.requireNonNull(file.getOriginalFilename(), "文件名不能为空");
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        if (!SUPPORTED_FORMATS.contains(suffix)) {
            return "【错误】不支持的文件格式（仅支持xml），当前文件：" + fileName;
        }

        // 3. 解析并格式化XML
        try {
            // 读取文件内容（UTF-8编码）
            String xmlContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            // 校验XML语法
            Object xmlNode = XML_MAPPER.readTree(xmlContent);
            // 格式化输出
            String formattedXml = XML_MAPPER.writeValueAsString(xmlNode);

            // 拼接RAG结构化内容
            StringBuilder ragContent = new StringBuilder();
            ragContent.append("=== XML文档元信息 ===\n");
            ragContent.append("文件名：").append(fileName).append("\n");
            ragContent.append("文件大小：").append(file.getSize()).append("字节\n");
            ragContent.append("字符编码：UTF-8\n\n");
            ragContent.append("=== 格式化XML内容 ===\n");
            ragContent.append(formattedXml);

            return ragContent.toString().trim();
        } catch (IOException e) {
            return "【错误】XML解析失败（语法错误/格式异常）：" + e.getMessage();
        }
    }
}
