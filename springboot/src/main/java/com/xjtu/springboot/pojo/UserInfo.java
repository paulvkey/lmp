package com.xjtu.springboot.pojo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserInfo implements Serializable {
    private Long id;
    private Long userId;
    private String username;
    private String avatar;
    private String email;
    private LocalDateTime birthday;
    private Byte sex;
    private Byte status;
    private String bio;
    private Byte pwdFailedCount;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private static final long serialVersionUID = 1L;
}