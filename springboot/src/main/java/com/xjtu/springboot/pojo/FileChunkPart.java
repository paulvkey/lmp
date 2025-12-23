package com.xjtu.springboot.pojo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class FileChunkPart implements Serializable {
    private Long id;
    private String uploadId;
    private Long chunkId;
    private Integer partNumber;
    private String eTag;
    private Long chunkSize;
    private String storagePath;
    private LocalDateTime uploadAt;

    @Serial
    private static final long serialVersionUID = 1L;

}