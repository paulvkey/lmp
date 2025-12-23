package com.xjtu.springboot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "file.upload")
public class FileConfig {
    // 文件存储根路径（替代硬编码的FILE_PATH）
    private String basePath = System.getProperty("user.dir") + "/files/";
    // 服务访问前缀
    private String serverPrefix = "http://localhost:8090";
    // 允许的文件类型
    private List<String> allowTypes = List.of("txt", "jpg", "jpeg", "png", "pdf", "doc", "docx", "xls", "xlsx");
    // 对话窗口上传文件最大大小
    private Long chatMaxFileSize = 104857600L;
    // 单文件最大大小(4G)
    private Long maxFileSize = 4294967296L;
    // 分片大小(100MB)
    private Long chunkSize = 104857600L;
    // 文件重复处理策略：rename-重命名，reject-拒绝，cover-覆盖
    private String duplicateStrategy = "cover";

    // 匿名文件存储子路径
    private String anonymSubPath = "anonym/";
    // 匿名文件过期时间（小时）
    private Integer anonymExpireHours = 24;
    // 是否允许匿名上传
    private Boolean allowAnonymUpload = true;
}
