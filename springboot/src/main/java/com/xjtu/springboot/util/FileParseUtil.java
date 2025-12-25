package com.xjtu.springboot.util;

import com.xjtu.springboot.util.parser.*;
import org.springframework.web.multipart.MultipartFile;


public class FileParseUtil {
    public static String parse(MultipartFile file) throws Exception {
        String ext = FileUtil.getExtension(file);
        return parse(file, ext);
    }

    public static String parse(MultipartFile file, String ext) throws Exception {
        return switch (ext) {
            case "pdf" -> PdfParseUtil.parse(file);
            case "doc", "docx" -> DocParseUtil.parse(file);
            case "xls", "xlsx" -> ExcelParseUtil.parse(file);
            case "md", "markdown" -> MdParseUtil.parse(file);
            case "json" -> JsonParseUtil.parse(file);
            case "xml" -> XmlParseUtil.parse(file);
            default -> TikaParseUtil.parse(file);
        };
    }

}