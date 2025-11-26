package com.xjtu.springboot.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class PwdData implements Serializable  {
    private static final long serialVersionUID = 1L;
    private String username;
    private String oldPwd;
    private String newPwd;
}
