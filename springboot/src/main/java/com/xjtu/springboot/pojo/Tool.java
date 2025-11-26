package com.xjtu.springboot.pojo;

import lombok.Data;

@Data
public class Tool {
    private static final long serialVersionUID = 1L;
    private String type = "function";
    private MsgFunction function;
}
