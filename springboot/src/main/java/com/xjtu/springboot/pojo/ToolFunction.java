package com.xjtu.springboot.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class ToolFunction implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String description;
    // json
    private Object parameters;
}
