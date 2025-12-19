package com.xjtu.springboot.pojo.common;

import lombok.Getter;

@Getter
public enum Role {
    SYSTEM("system", 0),
    USER("user", 1),
    ASSISTANT("assistant", 2),
    TOOL("tool", 3);

    final String name;
    final Integer role;

    Role(String name, Integer role) {
        this.name = name;
        this.role = role;
    }
}