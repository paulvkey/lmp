package com.xjtu.springboot.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Options {
    private static final long serialVersionUID = 1L;
    private Integer seed;
    private Double temperature;
    @JsonProperty("top_k")
    private Integer topK;
    @JsonProperty("top_p")
    private Double topP;
    @JsonProperty("min_p")
    private Double minP;
    private String stop;
    @JsonProperty("num_ctx")
    private Integer numCtx;
    @JsonProperty("num_predict")
    private Integer numPredict;
}
