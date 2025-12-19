package com.xjtu.springboot.dto.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serial;
import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String anonymId;
    private Long sessionId;
    private Long folderId;
    private String relativePath;
    private String storageType = "local";
    private String bucketName;
    private MultipartFile file;
}
