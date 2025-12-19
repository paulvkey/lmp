package com.xjtu.springboot.pojo.common;

import lombok.Getter;

@Getter
public enum MsgType {
    TEXT(1),
    FILE(2),
    IMAGE(3);

    final Integer type;
    MsgType(Integer type) {
        this.type = type;
    }

}