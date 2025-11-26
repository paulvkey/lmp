package com.xjtu.springboot.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class User implements Serializable {
    private Long id;
    private String username;
    private String phone;
    private String password;
    private String token;

    private static final long serialVersionUID = 1L;
}