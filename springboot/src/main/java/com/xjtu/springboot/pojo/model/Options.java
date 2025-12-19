package com.xjtu.springboot.pojo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class Options implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer seed = 12345;
    private Double temperature = 0.6;
    @JsonProperty("top_k")
    private Integer topK = 20;
    @JsonProperty("top_p")
    private Double topP = 0.95;
    @JsonProperty("min_p")
    private Double minP = 0.0;
    private String stop;
    @JsonProperty("num_ctx")
    private Integer numCtx = 4096;
    @JsonProperty("num_predict")
    private Integer numPredict = 4096;
}
