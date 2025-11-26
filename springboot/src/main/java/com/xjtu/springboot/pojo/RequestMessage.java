package com.xjtu.springboot.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RequestMessage {
    private static final long serialVersionUID = 1L;
    private String role;
    private String content;
    // Base64-encoded image content
    private List<String> images;
    @JsonProperty("tool_calls")
    private List<MsgFunction> toolCalls;
    @JsonProperty("tool_name")
    private String toolName;
}