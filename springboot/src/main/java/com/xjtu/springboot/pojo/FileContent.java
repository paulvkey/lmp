package com.xjtu.springboot.pojo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class FileContent implements Serializable {
    private Long id;
    private Long fileId;
    private String type;
    private String content;
    private LocalDateTime parseTime;

    @Serial
    private static final long serialVersionUID = 1L;
}