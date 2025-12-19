package com.xjtu.springboot.dto.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xjtu.springboot.pojo.model.Options;
import com.xjtu.springboot.pojo.model.RequestMessage;
import com.xjtu.springboot.pojo.model.ToolFunction;
import lombok.Data;

import java.util.List;

@Data
public class RequestDto {
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
