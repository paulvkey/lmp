package com.xjtu.springboot.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class Tool implements Serializable {
    private static final long serialVersionUID = 1L;
    private String type = "function";
    private MsgFunction function;
}
