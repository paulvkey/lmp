package com.xjtu.springboot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Msg implements Serializable {
    private static final long serialVersionUID = 1L;
    String thinking;
    String content;
    String type;
    // 1:用户 2：AI
    Integer role;
    String fileIds;

    public Msg(String thinking, String content, String type, Integer role, String fileIds) {
        this.thinking = thinking;
        this.content = content;
        this.type = type;
        this.role = role;
        this.fileIds = fileIds;
    }
}