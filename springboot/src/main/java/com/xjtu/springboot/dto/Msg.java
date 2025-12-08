package com.xjtu.springboot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Msg {
    private static final long serialVersionUID = 1L;
    String content;
    String type;
    // 1:用户 2：AI
    Integer role;

    public Msg(String content, String type, Integer role) {
        this.content = content;
        this.type = type;
        this.role = role;
    }
}