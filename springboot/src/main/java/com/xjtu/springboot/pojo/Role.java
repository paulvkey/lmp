package com.xjtu.springboot.pojo;

public enum Role {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool");

    private String name;

    Role(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}