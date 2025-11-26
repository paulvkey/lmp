package com.xjtu.springboot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfoData implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long userId;

    private String username;

    private String phone;

    private String password;

    private String email;

    private String avatar;

    private Byte sex;

    private LocalDateTime birthday;

    private Byte status;

    private String bio;

    private Byte pwdFailedCount;

    private LocalDateTime lastLoginTime;

    private String lastLoginIp;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String token;
}
