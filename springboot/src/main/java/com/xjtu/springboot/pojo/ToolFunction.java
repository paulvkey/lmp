package com.xjtu.springboot.pojo;

import lombok.Data;

@Data
public class ToolFunction {
    private static final long serialVersionUID = 1L;
    private String name;
    private String description;
    // json
    private Object parameters;
}
