package com.xjtu.springboot.util.parser;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 高性能Markdown解析工具（适配RAG核心样式 + 极致性能）
 * 核心：预编译正则 + 行级状态机 + 语义提取（过滤视觉噪声）
 */
public class MdParseUtil {

    // ========== 预编译正则（RAG核心样式） ==========
    // 基础结构
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.*)$"); // 标题
    private static final Pattern CODE_BLOCK_START = Pattern.compile("^```(.*)$"); // 代码块开始
    private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("^([-*]|\\d+\\.)\\s+(.*)$"); // 普通列表项
    private static final Pattern TABLE_ROW_PATTERN = Pattern.compile("^\\|.*\\|$"); // 表格行
    // 扩展样式
    private static final Pattern BLOCKQUOTE_PATTERN = Pattern.compile("^>\\s*(.*)$"); // 块引用
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[(.*?)\\]\\((.*?)\\)"); // 链接
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[(.*?)\\]\\((.*?)\\)"); // 图片
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*"); // 粗体
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*(.*?)\\*"); // 斜体
    private static final Pattern STRIKETHROUGH_PATTERN = Pattern.compile("~~(.*?)~~"); // 删除线
    private static final Pattern TASK_LIST_PATTERN = Pattern.compile("^[-*]\\s*\\[(x| )\\]\\s*(.*)$"); // 任务列表
    private static final Pattern FOOTNOTE_MARK_PATTERN = Pattern.compile("\\[\\^(\\d+)\\]"); // 脚注标记
    private static final Pattern FOOTNOTE_CONTENT_PATTERN = Pattern.compile("^\\[\\^(\\d+)\\]:\\s*(.*)$"); // 脚注内容

    /**
     * 核心方法：解析MD文件，高性能+适配全量RAG核心样式
     */
    public static String parseMarkdownForRAG(MultipartFile file) {
        // 1. 参数校验
        if (file == null || file.isEmpty()) {
            return "【错误】上传的Markdown文件为空";
        }
        String fileName = Objects.requireNonNull(file.getOriginalFilename(), "文件名不能为空");
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        if (!Set.of("md", "markdown").contains(suffix)) {
            return "【错误】不支持的文件格式（仅支持md/markdown），当前文件：" + fileName;
        }

        // 2. 读取MD内容（按行处理，高性能）
        try {
            String mdContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            String[] lines = mdContent.split("\\r?\\n"); // 兼容所有换行符

            StringBuilder ragContent = new StringBuilder();
            // 元信息
            ragContent.append("=== Markdown文档元信息 ===\n");
            ragContent.append("文件名：").append(fileName).append("\n");
            ragContent.append("文件大小：").append(file.getSize()).append("字节\n");
            ragContent.append("字符编码：UTF-8\n\n");
            ragContent.append("=== 结构化Markdown内容 ===\n");

            // 状态机（轻量化，仅标记跨多行结构）
            boolean inCodeBlock = false;
            String codeLanguage = "";
            List<String> tableRows = new ArrayList<>();
            Map<Integer, String> footnoteMap = new HashMap<>(); // 脚注缓存（标记→内容）

            // 逐行解析（核心高性能逻辑）
            for (String line : lines) {
                String trimLine = line.trim();
                // 空行：处理未闭合的表格/重置状态
                if (trimLine.isEmpty()) {
                    if (!tableRows.isEmpty()) {
                        ragContent.append("【表格】\n").append(String.join("\n", tableRows)).append("\n\n");
                        tableRows.clear();
                    }
                    continue;
                }

                // ========== 1. 处理跨多行结构：代码块 ==========
                if (inCodeBlock) {
                    if (trimLine.equals("```")) { // 代码块结束
                        ragContent.append("```\n\n");
                        inCodeBlock = false;
                        codeLanguage = "";
                    } else { // 代码块内容：直接保留（RAG需完整代码逻辑）
                        ragContent.append("  ").append(line).append("\n");
                    }
                    continue;
                }

                // ========== 2. 匹配脚注内容（先缓存，最后统一输出） ==========
                Matcher footnoteContentMatcher = FOOTNOTE_CONTENT_PATTERN.matcher(trimLine);
                if (footnoteContentMatcher.find()) {
                    int footnoteId = Integer.parseInt(footnoteContentMatcher.group(1));
                    String footnoteContent = footnoteContentMatcher.group(2);
                    footnoteMap.put(footnoteId, footnoteContent);
                    continue;
                }

                // ========== 3. 匹配标题 ==========
                Matcher headingMatcher = HEADING_PATTERN.matcher(line);
                if (headingMatcher.find()) {
                    int level = headingMatcher.group(1).length();
                    String titleContent = cleanInlineStyles(headingMatcher.group(2).trim()); // 清洗行内样式（粗体/斜体等）
                    ragContent.append("【标题").append(level).append("】").append(titleContent).append("\n\n");
                    continue;
                }

                // ========== 4. 匹配代码块开始 ==========
                Matcher codeStartMatcher = CODE_BLOCK_START.matcher(trimLine);
                if (codeStartMatcher.find()) {
                    inCodeBlock = true;
                    codeLanguage = codeStartMatcher.group(1).trim();
                    ragContent.append("【代码块（").append(codeLanguage.isEmpty() ? "无语言" : codeLanguage).append("）】\n");
                    ragContent.append("```").append(codeLanguage).append("\n");
                    continue;
                }

                // ========== 5. 匹配表格行 ==========
                if (TABLE_ROW_PATTERN.matcher(line).find()) {
                    tableRows.add(line);
                    continue;
                }

                // ========== 6. 匹配块引用 ==========
                Matcher blockquoteMatcher = BLOCKQUOTE_PATTERN.matcher(line);
                if (blockquoteMatcher.find()) {
                    String quoteContent = cleanInlineStyles(blockquoteMatcher.group(1).trim());
                    ragContent.append("【块引用】").append(quoteContent).append("\n\n");
                    continue;
                }

                // ========== 7. 匹配任务列表 ==========
                Matcher taskListMatcher = TASK_LIST_PATTERN.matcher(line);
                if (taskListMatcher.find()) {
                    String status = taskListMatcher.group(1).equals("x") ? "完成" : "未完成";
                    String taskContent = cleanInlineStyles(taskListMatcher.group(2).trim());
                    ragContent.append("【任务列表】").append(status).append("：").append(taskContent).append("\n");
                    continue;
                }

                // ========== 8. 匹配普通列表项 ==========
                Matcher listItemMatcher = LIST_ITEM_PATTERN.matcher(line);
                if (listItemMatcher.find()) {
                    String listType = listItemMatcher.group(1).matches("\\d+\\.") ? "有序列表" : "无序列表";
                    String listContent = cleanInlineStyles(listItemMatcher.group(2).trim());
                    // 替换脚注标记为实际内容
                    listContent = replaceFootnote(listContent, footnoteMap);
                    ragContent.append("【").append(listType).append("项】").append(listContent).append("\n");
                    continue;
                }

                // ========== 9. 普通段落（清洗所有行内样式 + 替换脚注） ==========
                String paragraphContent = cleanInlineStyles(trimLine);
                paragraphContent = replaceFootnote(paragraphContent, footnoteMap);
                ragContent.append("【段落】").append(paragraphContent).append("\n\n");
            }

            // ========== 处理末尾未闭合的结构 ==========
            if (!tableRows.isEmpty()) {
                ragContent.append("【表格】\n").append(String.join("\n", tableRows)).append("\n\n");
            }
            if (inCodeBlock) {
                ragContent.append("```\n");
            }

            // ========== 输出脚注（RAG需完整语义） ==========
            if (!footnoteMap.isEmpty()) {
                ragContent.append("【脚注】\n");
                footnoteMap.forEach((id, content) -> ragContent.append(id).append("：").append(content).append("\n"));
                ragContent.append("\n");
            }

            return ragContent.toString().trim();
        } catch (IOException e) {
            return "【错误】Markdown解析失败：" + e.getMessage();
        }
    }

    /**
     * 清洗行内样式（粗体/斜体/删除线，保留语义标注，去除语法符号）
     */
    private static String cleanInlineStyles(String content) {
        // 粗体：**内容** → 【粗体】内容
        Matcher boldMatcher = BOLD_PATTERN.matcher(content);
        content = boldMatcher.replaceAll("【粗体】$1【/粗体】");

        // 斜体：*内容* → 【斜体】内容
        Matcher italicMatcher = ITALIC_PATTERN.matcher(content);
        content = italicMatcher.replaceAll("【斜体】$1【/斜体】");

        // 删除线：~~内容~~ → 【删除线】内容
        Matcher strikethroughMatcher = STRIKETHROUGH_PATTERN.matcher(content);
        content = strikethroughMatcher.replaceAll("【删除线】$1【/删除线】");

        // 链接：[文本](url) → 【链接】文本（URL：url）
        Matcher linkMatcher = LINK_PATTERN.matcher(content);
        content = linkMatcher.replaceAll("【链接】$1（URL：$2）");

        // 图片：![描述](path) → 【图片】描述（路径：path）
        Matcher imageMatcher = IMAGE_PATTERN.matcher(content);
        content = imageMatcher.replaceAll("【图片】$1（路径：$2）");

        return content;
    }

    /**
     * 替换脚注标记为实际内容（[1] → 脚注1：xxx）
     */
    private static String replaceFootnote(String content, Map<Integer, String> footnoteMap) {
        Matcher footnoteMarkMatcher = FOOTNOTE_MARK_PATTERN.matcher(content);
        return footnoteMarkMatcher.replaceAll(match -> {
            int id = Integer.parseInt(match.group(1));
            return footnoteMap.containsKey(id) ? "【脚注" + id + "】" + footnoteMap.get(id) : match.group(0);
        });
    }
}