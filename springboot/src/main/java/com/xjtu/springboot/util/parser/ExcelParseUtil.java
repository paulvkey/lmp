package com.xjtu.springboot.util.parser;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExcelParseUtil {

    // 支持的Excel格式
    private static final Set<String> SUPPORTED_FORMATS = new HashSet<>(Arrays.asList("xls", "xlsx"));
    // 日期格式化（统一日期显示格式）
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 核心方法：解析xls/xlsx，返回RAG友好的结构化字符串
     * @param file 上传的Excel文件
     * @return RAG可用的结构化字符串（工作表+Markdown表格）
     */
    public static String extractExcelForRAG(MultipartFile file) {
        // 1. 参数校验
        if (file == null || file.isEmpty()) {
            return "【错误】上传的Excel文件为空";
        }

        // 2. 校验文件格式
        String fileName = Objects.requireNonNull(file.getOriginalFilename(), "文件名不能为空");
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        if (!SUPPORTED_FORMATS.contains(suffix)) {
            return "【错误】不支持的文件格式（仅支持xls/xlsx），当前文件：" + fileName;
        }

        // 3. 解析Excel文件
        Workbook workbook = null;
        try {
            // 根据格式创建Workbook
            if ("xls".equals(suffix)) {
                workbook = new HSSFWorkbook(file.getInputStream());
            } else if ("xlsx".equals(suffix)) {
                workbook = new XSSFWorkbook(file.getInputStream());
            }

            StringBuilder ragContent = new StringBuilder();
            // 元信息
            ragContent.append("=== Excel文档元信息 ===\n");
            ragContent.append("文件名：").append(fileName).append("\n");
            ragContent.append("文件大小：").append(file.getSize()).append("字节\n");
            ragContent.append("文件格式：").append(suffix.toUpperCase()).append("\n");
            ragContent.append("工作表数量：").append(workbook.getNumberOfSheets()).append("\n\n");

            // 遍历每个工作表
            for (int sheetIdx = 0; sheetIdx < workbook.getNumberOfSheets(); sheetIdx++) {
                Sheet sheet = workbook.getSheetAt(sheetIdx);
                String sheetName = workbook.getSheetName(sheetIdx);
                int rowCount = sheet.getPhysicalNumberOfRows();

                ragContent.append("=== 工作表：").append(sheetName).append(" ===\n");
                ragContent.append("总行数：").append(rowCount).append("\n");

                // 过滤空工作表
                if (rowCount == 0) {
                    ragContent.append("内容：空工作表\n\n");
                    continue;
                }

                // 提取表格数据（转为Markdown）
                List<List<String>> tableData = extractSheetData(sheet);
                ragContent.append("【表格内容】\n");
                ragContent.append(convertTableToMarkdown(tableData)).append("\n\n");
            }

            return ragContent.toString().trim();
        } catch (IOException e) {
            return "【错误】Excel解析失败：" + e.getMessage();
        } finally {
            // 关闭资源
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) { /* 关闭失败不影响结果 */ }
            }
        }
    }

    /**
     * 提取单个工作表的结构化数据（表头+行）
     */
    private static List<List<String>> extractSheetData(Sheet sheet) {
        List<List<String>> tableData = new ArrayList<>();
        // 遍历所有行
        for (Row row : sheet) {
            List<String> rowData = new ArrayList<>();
            // 遍历当前行的单元格
            for (Cell cell : row) {
                String cellValue = getCellValue(cell).trim();
                rowData.add(cellValue);
            }
            // 过滤空行（所有单元格都为空）
            if (!rowData.stream().allMatch(String::isEmpty)) {
                tableData.add(rowData);
            }
        }
        return tableData;
    }

    /**
     * 解析单元格值（彻底修正类型错误，兼容所有单元格类型）
     * 关键：所有分支返回String，入参仅为Cell类型，无类型混淆
     */
    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        // 先处理公式单元格：获取公式计算后的实际值
        if (cell.getCellType() == CellType.FORMULA) {
            // 获取公式表达式
            String formula = cell.getCellFormula();
            // 获取公式计算结果（自动适配结果类型）
            String formulaResult = getFormulaCellResult(cell);
            return formula + "=" + formulaResult;
        }

        // 处理非公式单元格
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // 正确调用DateUtil：参数为Cell类型
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    // 格式化日期，避免默认格式混乱
                    return DATE_FORMAT.format(cell.getDateCellValue());
                } else {
                    // 处理数字：整数去小数位，浮点数保留原始精度
                    double numericValue = cell.getNumericCellValue();
                    return numericValue == Math.floor(numericValue)
                            ? String.valueOf((long) numericValue)
                            : String.valueOf(numericValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    /**
     * 单独处理公式单元格的计算结果（避免类型混淆）
     */
    private static String getFormulaCellResult(Cell formulaCell) {
        switch (formulaCell.getCachedFormulaResultType()) {
            case STRING:
                return formulaCell.getStringCellValue();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(formulaCell)) {
                    return DATE_FORMAT.format(formulaCell.getDateCellValue());
                } else {
                    double value = formulaCell.getNumericCellValue();
                    return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
                }
            case BOOLEAN:
                return String.valueOf(formulaCell.getBooleanCellValue());
            case BLANK:
                return "";
            default:
                return "计算失败";
        }
    }

    /**
     * 二维表格转Markdown（RAG友好）
     */
    private static String convertTableToMarkdown(List<List<String>> tableData) {
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
            // 补全列数（避免表格格式错乱）
            while (row.size() < header.size()) {
                row.add("");
            }
            mdTable.append("| ").append(String.join(" | ", row)).append(" |\n");
        }
        return mdTable.toString();
    }
}
