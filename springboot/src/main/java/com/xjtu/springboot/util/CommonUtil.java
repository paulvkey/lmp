package com.xjtu.springboot.util;

import java.util.UUID;

public class CommonUtil {
    // 生成无横线的UUID
    public static String getUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // 字节数组转16进制字符串
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
