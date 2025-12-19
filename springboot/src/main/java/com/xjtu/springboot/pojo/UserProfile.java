package com.xjtu.springboot.pojo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserProfile implements Serializable {
    private Long id;
    private Long userId;
    private String username;
    private String avatar;
    private Byte sex;
    private String email;
    private LocalDateTime birthday;
    private String bio;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Serial
    private static final long serialVersionUID = 1L;
}