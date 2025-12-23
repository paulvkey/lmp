package com.xjtu.springboot.util.parser;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableIterator;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

public class DocParseUtil {
    // 支持的文件格式
    private static final Set<String> SUPPORTED_FORMATS = new HashSet<>(Arrays.asList("doc", "docx"));

    public static String parse(MultipartFile file) {
        // 参数校验
        if (file == null || file.isEmpty()) {
            return "【错误】上传的Word文件为空";
        }

        // 校验文件格式
        String fileName = Objects.requireNonNull(file.getOriginalFilename(), "文件名不能为空");
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        if (!SUPPORTED_FORMATS.contains(suffix)) {
            return "【错误】不支持的文件格式（仅支持doc/docx），当前文件：" + fileName;
        }

        // 解析文件
        try {
            StringBuilder ragContent = new StringBuilder();
            // 元信息
            ragContent.append("=== Word文档元信息 ===\n");
            ragContent.append("文件名：").append(fileName).append("\n");
            ragContent.append("文件大小：").append(file.getSize()).append("字节\n");
            ragContent.append("文件格式：").append(suffix.toUpperCase()).append("\n\n");

            if ("doc".equals(suffix)) {
                parseDocContent(file, ragContent);
            } else if ("docx".equals(suffix)) {
                parseDocxContent(file, ragContent);
            }

            return ragContent.toString().trim();
        } catch (IOException e) {
            return "【错误】Word解析失败：" + e.getMessage();
        }
    }

    /**
     * 解析doc文件（HWPF）
     */
    private static void parseDocContent(MultipartFile file, StringBuilder ragContent) throws IOException {
        try (HWPFDocument doc = new HWPFDocument(file.getInputStream())) {
            // 提取文本
            WordExtractor extractor = new WordExtractor(doc);
            String[] paragraphs = extractor.getParagraphText();
            ragContent.append("=== 文本内容 ===\n");
            for (int i = 0; i < paragraphs.length; i++) {
                String para = paragraphs[i].trim();
                if (!para.isEmpty()) {
                    ragContent.append("段落").append(i + 1).append("：").append(para).append("\n");
                }
            }
            ragContent.append("\n");

            // 提取表格并转为Markdown
            TableIterator tableIterator = new TableIterator(doc.getRange());
            int tableCount = 0;
            while (tableIterator.hasNext()) {
                tableCount++;
                Table table = tableIterator.next();
                List<List<String>> tableData = new ArrayList<>();

                // 遍历表格行
                for (int i = 0; i < table.numRows(); i++) {
                    TableRow row = table.getRow(i);
                    List<String> rowData = new ArrayList<>();
                    // 遍历单元格
                    for (int j = 0; j < row.numCells(); j++) {
                        TableCell cell = row.getCell(j);
                        String cellText = cell.text().trim().replaceAll("\\s+", " ");
                        rowData.add(cellText);
                    }
                    tableData.add(rowData);
                }

                // 追加表格（Markdown格式）
                ragContent.append("=== 表格").append(tableCount).append(" ===\n");
                ragContent.append(parseTableToMarkdown(tableData)).append("\n\n");
            }

            if (tableCount == 0) {
                ragContent.append("=== 表格内容 ===\n无表格\n");
            }
        }
    }

    /**
     * 解析docx文件（XWPF）
     */
    private static void parseDocxContent(MultipartFile file, StringBuilder ragContent) throws IOException {
        try (XWPFDocument docx = new XWPFDocument(file.getInputStream())) {
            // 提取文本
            XWPFWordExtractor extractor = new XWPFWordExtractor(docx);
            String text = extractor.getText().trim();
            ragContent.append("=== 文本内容 ===\n");
            // 按段落拆分（空行分隔）
            String[] paragraphs = text.split("\\n\\s*\\n");
            for (int i = 0; i < paragraphs.length; i++) {
                String para = paragraphs[i].trim();
                if (!para.isEmpty()) {
                    ragContent.append("段落").append(i + 1).append("：").append(para).append("\n");
                }
            }
            ragContent.append("\n");

            // 提取表格并转为Markdown
            List<XWPFTable> tables = docx.getTables();
            ragContent.append("=== 表格内容 ===\n");
            if (tables.isEmpty()) {
                ragContent.append("无表格\n");
                return;
            }

            for (int tableIdx = 0; tableIdx < tables.size(); tableIdx++) {
                XWPFTable table = tables.get(tableIdx);
                List<List<String>> tableData = new ArrayList<>();

                // 遍历表格行
                table.getRows().forEach(row -> {
                    List<String> rowData = new ArrayList<>();
                    // 遍历单元格
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getText().trim().replaceAll("\\s+", " ");
                        rowData.add(cellText);
                    }
                    tableData.add(rowData);
                });

                // 追加表格
                ragContent.append("表格").append(tableIdx + 1).append("：\n");
                ragContent.append(parseTableToMarkdown(tableData)).append("\n");
            }
        }
    }

    /**
     * 二维表格转Markdown
     */
    private static String parseTableToMarkdown(List<List<String>> tableData) {
        if (tableData.isEmpty()) {
            return "空表格";
        }

        StringBuilder mdTable = new StringBuilder();
        // 表头（默认第一行为表头）
        List<String> header = tableData.get(0);
        mdTable.append("| ").append(String.join(" | ", header)).append(" |\n");
        // 分隔线
        mdTable.append("| ").append(String.join(" | ", Collections.nCopies(header.size(), "-"))).append(" |\n");
        // 表体
        for (int i = 1; i < tableData.size(); i++) {
            List<String> row = tableData.get(i);
            // 补全列数（避免格式错乱）
            while (row.size() < header.size()) {
                row.add("");
            }
            mdTable.append("| ").append(String.join(" | ", row)).append(" |\n");
        }
        return mdTable.toString();
    }
}
