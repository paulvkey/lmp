package com.xjtu.springboot.dto.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RagFileDto implements Serializable {
    private Long userId;
    private Long sessionId;
    private String anonymousId;
    private Long fileId;
    private Long folderId;
    private String fileMd5;
    private String storageType;
    private Boolean succeed = false;

    @Serial
    private static final long serialVersionUID = 1L;
}
