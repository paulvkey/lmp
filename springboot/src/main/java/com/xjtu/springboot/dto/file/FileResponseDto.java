package com.xjtu.springboot.dto.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileResponseDto implements Serializable {
    private Long fileId;
    private Long folderId;
    private Boolean succeed = false;
    private String accessUrl;
    // 已上传分片索引（断点续传）
    private List<Integer> uploadedChunk;

    @Serial
    private static final long serialVersionUID = 1L;

}
