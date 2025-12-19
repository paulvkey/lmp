package com.xjtu.springboot.util.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PdfParseUtil {
    /**
     * 核心方法：提取PDF文本+表格，返回RAG友好的结构化字符串
     * @param file 上传的PDF文件
     * @return RAG可用的结构化字符串（文本+Markdown表格）
     */
    public static String extractPdfForRAG(MultipartFile file) {
        // 1. 参数校验
        if (file == null || file.isEmpty()) {
            return "【错误】上传的PDF文件为空";
        }
        String fileName = Objects.requireNonNull(file.getOriginalFilename(), "文件名不能为空");
        if (!fileName.toLowerCase().endsWith(".pdf")) {
            return "【错误】文件不是PDF格式（文件名：" + fileName + "）";
        }

        // 2. 校验PDF魔数（精准判断）
        try (var is = file.getInputStream()) {
            byte[] header = new byte[4];
            if (is.read(header) != 4 || !"%PDF".equals(new String(header, StandardCharsets.ISO_8859_1))) {
                return "【错误】文件不是有效的PDF格式（魔数校验失败）";
            }
        } catch (IOException e) {
            return "【错误】文件校验失败：" + e.getMessage();
        }

        // 3. 解析PDF文本和表格
        PDDocument document = null;
        try {
            document = Loader.loadPDF(file.getBytes());
            int totalPages = document.getNumberOfPages();
            StringBuilder ragContent = new StringBuilder();

            // 全局信息
            ragContent.append("=== PDF文档元信息 ===\n");
            ragContent.append("文件名：").append(fileName).append("\n");
            ragContent.append("文件大小：").append(file.getSize()).append("字节\n");
            ragContent.append("总页数：").append(totalPages).append("\n\n");

            // 遍历每一页提取内容
            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                ragContent.append("=== 第").append(pageNum).append("页内容 ===\n");

                // 3.1 提取当前页文本
                String pageText = extractPageText(document, pageNum);
                ragContent.append("【文本内容】\n").append(pageText).append("\n");

                // 3.2 提取当前页表格并转为Markdown格式
                List<List<String>> pageTable = extractPageTable(document, pageNum);
                if (!pageTable.isEmpty()) {
                    ragContent.append("【表格内容】\n").append(convertTableToMarkdown(pageTable)).append("\n");
                }

                ragContent.append("\n"); // 页间分隔
            }

            return ragContent.toString().trim();
        } catch (IOException e) {
            return "【错误】PDF解析失败：" + e.getMessage();
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) { /* 关闭失败不影响结果 */ }
            }
        }
    }

    /**
     * 提取指定页码的文本
     */
    private static String extractPageText(PDDocument document, int pageNum) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(pageNum);
        stripper.setEndPage(pageNum);
        stripper.setLineSeparator("\n");
        stripper.setSortByPosition(true); // 按文本位置排序，保证语义连贯
        return stripper.getText(document).trim();
    }

    /**
     * 提取指定页码的表格（基于坐标聚类）
     */
    private static List<List<String>> extractPageTable(PDDocument document, int pageNum) throws IOException {
        Map<Float, Map<Float, String>> cellMap = new TreeMap<>(); // Y轴（行）→ X轴（列）→ 文本

        PDFTextStripper stripper = new PDFTextStripper() {
            @Override
            protected void writeString(String text, List<TextPosition> textPositions) {
                if (text == null || text.trim().isEmpty()) {
                    return;
                }
                TextPosition tp = textPositions.get(0);
                // 坐标聚类（容错10px，适配大多数PDF表格）
                float rowY = Math.round(tp.getY() / 10) * 10;
                float colX = Math.round(tp.getX() / 10) * 10;

                // 合并同一单元格的文本
                String cellText = cellMap.getOrDefault(rowY, new TreeMap<>()).getOrDefault(colX, "");
                cellText += text.trim() + " ";
                cellMap.computeIfAbsent(rowY, k -> new TreeMap<>()).put(colX, cellText.trim());
            }
        };

        stripper.setStartPage(pageNum);
        stripper.setEndPage(pageNum);
        stripper.getText(document); // 触发解析

        // 转换为二维表格结构
        List<List<String>> table = new ArrayList<>();
        for (Map<Float, String> row : cellMap.values()) {
            table.add(new ArrayList<>(row.values()));
        }
        return table;
    }

    /**
     * 将二维表格转为Markdown格式（RAG友好）
     */
    private static String convertTableToMarkdown(List<List<String>> table) {
        if (table.isEmpty()) {
            return "";
        }

        StringBuilder mdTable = new StringBuilder();
        // 表头（默认第一行为表头）
        List<String> header = table.get(0);
        mdTable.append("| ").append(String.join(" | ", header)).append(" |\n");
        // 分隔线
        mdTable.append("| ").append(String.join(" | ", Collections.nCopies(header.size(), "-"))).append(" |\n");
        // 表体
        for (int i = 1; i < table.size(); i++) {
            List<String> row = table.get(i);
            // 补全列数（避免表格格式错乱）
            while (row.size() < header.size()) {
                row.add("");
            }
            mdTable.append("| ").append(String.join(" | ", row)).append(" |\n");
        }
        return mdTable.toString();
    }
}
