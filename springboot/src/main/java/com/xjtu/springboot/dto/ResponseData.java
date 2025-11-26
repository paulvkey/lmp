package com.xjtu.springboot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xjtu.springboot.pojo.ResponseMessage;
import lombok.Data;

@Data
public class ResponseData {
    private String model;
    @JsonProperty("created_at")
    private String createdAt;
    private ResponseMessage message;
    private Boolean done;
    @JsonProperty("done_reason")
    private String doneReason;
    @JsonProperty("total_duration")
    private Long totalDuration;
    @JsonProperty("load_duration")
    private Long loadDuration;
    @JsonProperty("prompt_eval_count")
    private Long promptEvalCount;
    @JsonProperty("prompt_eval_duration")
    private Long promptEvalDuration;
    @JsonProperty("eval_count")
    private Long evalCount;
    @JsonProperty("eval_duration")
    private Long evalDuration;
}
