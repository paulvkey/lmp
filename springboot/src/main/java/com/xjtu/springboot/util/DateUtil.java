package com.xjtu.springboot.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    // 格式化器（线程安全）
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // 显式指定时区（北京时间）
    private static final ZoneId zoneId = ZoneId.of("Asia/Shanghai");

    public static String format(LocalDateTime dateTime) {
        return dateTime.atZone(zoneId).format(formatter);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(zoneId);
    }

    public static LocalDateTime now(String format) {
        return LocalDateTime.parse(format, formatter);
    }

    public static long nowMs(LocalDateTime dateTime) {
        return dateTime.atZone(zoneId)
                .toInstant()
                .toEpochMilli();
    }
}
