package com.xjtu.springboot.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class AiModel implements Serializable {
    private Long id;
    private String modelName;
    private String modelCode;
    private String modelDesc;
    private Byte isDefault;
    private Byte status;
    private Integer maxToken;
    private String prompt;

    private static final long serialVersionUID = 1L;
}