package com.xjtu.springboot.pojo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ResponseMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private String role;
    private String content;
    private String thinking;
    @JsonProperty("tool_calls")
    private List<MsgFunction> toolCalls;
    // Base64-encoded image content
    private List<String> images;
}