package com.xjtu.springboot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xjtu.springboot.pojo.Options;
import com.xjtu.springboot.pojo.RequestMessage;
import com.xjtu.springboot.pojo.ToolFunction;
import lombok.Data;

import java.util.List;

@Data
public class RequestData {
    private String model = "qwen3:8b";
    private List<RequestMessage> messages;
    private List<ToolFunction> tools;
    private String format;
    private Options options;
    private Boolean stream = true;
    private Boolean think = false;
    @JsonProperty("keep_alive")
    private String keepAlive = "30m";
}
