package com.xjtu.springboot.pojo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class Model implements Serializable {
    private Long id;
    private String name;
    private String desc;
    private Byte isDefault;
    private Byte status;
    private Integer maxToken;
    private String prompt;

    @Serial
    private static final long serialVersionUID = 1L;
}