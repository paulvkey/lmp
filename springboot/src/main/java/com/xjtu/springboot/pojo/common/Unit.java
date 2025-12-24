package com.xjtu.springboot.pojo.common;

import lombok.Getter;

@Getter
public enum Unit {
    M((long)(1024 * 1024)),
    G((long)(1024 * 1024 * 1024));

    private final Long step;

    Unit(Long step) {
        this.step = step;
    }
}
