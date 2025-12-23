package com.xjtu.springboot.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtil {
    // 格式化器（线程安全）
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // 显式指定时区（北京时间）
    private static final ZoneId zoneId = ZoneId.of("Asia/Shanghai");
    // 默认时间单位（分钟）
    private static final ChronoUnit DEFAULT_UNIT = ChronoUnit.MINUTES;

    /**
     * 时间差值比较类型枚举（
     * GT：目标时间-当前时间 > 时长（对应afterNow）
     * LT：当前时间-目标时间 > 时长（对应beforeNow）
     * EQ：时间差值 = 时长（对应equalNow）
     */
    public enum TimeDiffType {
        GT,
        LT,
        EQ
    }

    /**
     * 格式化LocalDateTime为指定格式的字符串（北京时间）
     * @param dateTime 待格式化的时间（null返回null）
     * @return 格式化后的字符串（yyyy-MM-dd HH:mm:ss）
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(zoneId).format(formatter);
    }

    /**
     * 获取当前北京时间的LocalDateTime
     * @return 当前北京时间（含年、月、日、时、分、秒、纳秒）
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(zoneId);
    }

    /**
     * 将指定格式的字符串解析为LocalDateTime（北京时间）
     * @param timeStr 待解析的时间字符串（yyyy-MM-dd HH:mm:ss，null/空返回null）
     * @return 解析后的LocalDateTime
     */
    public static LocalDateTime parse(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(timeStr, formatter);
    }

    /**
     * 获取当前北京时间【多久之后】的时间（默认单位：分钟）
     * 示例：plusTime(5) → 当前时间5分钟后；plusTime(1, ChronoUnit.HOURS) → 当前时间1小时后
     * @param duration 偏移时长（≥0）
     * @return 偏移后的北京时间
     */
    public static LocalDateTime plusTime(Integer duration) {
        return plusTime(duration, DEFAULT_UNIT);
    }

    /**
     * 重载：指定时间单位，获取当前北京时间【多久之后】的时间
     * @param duration 偏移时长（≥0）
     * @param unit 时间单位（非空，如ChronoUnit.SECONDS/HOURS/DAYS）
     * @return 偏移后的北京时间
     * @throws IllegalArgumentException 参数不合法时抛出
     */
    public static LocalDateTime plusTime(Integer duration, ChronoUnit unit) {
        validateOffsetParams(duration, unit);
        LocalDateTime now = LocalDateTime.now(zoneId);
        return now.plus(duration, unit);
    }

    /**
     * 获取当前北京时间【多久之前】的时间（默认单位：分钟）
     * 示例：minusTime(5) → 当前时间5分钟前；minusTime(1, ChronoUnit.DAYS) → 当前时间1天前
     * @param duration 偏移时长（≥0）
     * @return 偏移后的北京时间
     */
    public static LocalDateTime minusTime(Integer duration) {
        return minusTime(duration, DEFAULT_UNIT);
    }

    /**
     * 重载：指定时间单位，获取当前北京时间【多久之前】的时间
     * @param duration 偏移时长（≥0）
     * @param unit 时间单位（非空，如ChronoUnit.SECONDS/HOURS/DAYS）
     * @return 偏移后的北京时间
     * @throws IllegalArgumentException 参数不合法时抛出
     */
    public static LocalDateTime minusTime(Integer duration, ChronoUnit unit) {
        validateOffsetParams(duration, unit);
        LocalDateTime now = LocalDateTime.now(zoneId);
        return now.minus(duration, unit);
    }

    /**
     * 目标时间是否在当前时间之后，且差值大于指定分钟数
     * 示例：afterNow(target, 5) → 目标时间比当前时间晚超过5分钟
     * @param targetTime 目标时间（非空）
     * @param duration   比较的分钟数（≥0）
     * @return true=符合，false=不符合
     */
    public static boolean afterNow(LocalDateTime targetTime, Integer duration) {
        return compareTimeDiff(targetTime, duration, TimeDiffType.GT);
    }

    /**
     * 重载：指定时间单位，判断目标时间是否在当前时间之后且差值大于指定时长
     * @param targetTime 目标时间（非空）
     * @param duration   比较的时长（≥0）
     * @param unit       时间单位（非空，如ChronoUnit.SECONDS/HOURS）
     * @return true=符合，false=不符合
     */
    public static boolean afterNow(LocalDateTime targetTime, Integer duration, ChronoUnit unit) {
        return compareTimeDiff(targetTime, duration, unit, TimeDiffType.GT);
    }

    /**
     * 目标时间是否在当前时间之前，且差值大于指定分钟数
     * 示例：beforeNow(target, 5) → 目标时间比当前时间早超过5分钟
     * @param targetTime 目标时间（非空）
     * @param duration   比较的分钟数（≥0）
     * @return true=符合，false=不符合
     */
    public static boolean beforeNow(LocalDateTime targetTime, Integer duration) {
        return compareTimeDiff(targetTime, duration, TimeDiffType.LT);
    }

    /**
     * 重载：指定时间单位，判断目标时间是否在当前时间之前且差值大于指定时长
     * @param targetTime 目标时间（非空）
     * @param duration   比较的时长（≥0）
     * @param unit       时间单位（非空，如ChronoUnit.SECONDS/HOURS）
     * @return true=符合，false=不符合
     */
    public static boolean beforeNow(LocalDateTime targetTime, Integer duration, ChronoUnit unit) {
        return compareTimeDiff(targetTime, duration, unit, TimeDiffType.LT);
    }

    /**
     * 目标时间是否等于当前时间（差值为0）
     * @param targetTime 目标时间（非空）
     * @return true=相等，false=不相等
     */
    public static boolean equalNow(LocalDateTime targetTime) {
        // EQ类型默认duration=0，无需传入
        return compareTimeDiff(targetTime, 0, TimeDiffType.EQ);
    }

    /**
     * 重载：目标时间与当前时间的差值是否等于指定时长（默认单位：分钟）
     * @param targetTime 目标时间（非空）
     * @param duration   比较的分钟数（≥0）
     * @return true=差值等于duration，false=不等于
     */
    public static boolean equalNow(LocalDateTime targetTime, Integer duration) {
        return compareTimeDiff(targetTime, duration, TimeDiffType.EQ);
    }

    /**
     * 重载：指定时间单位，判断目标时间与当前时间的差值是否等于指定时长
     * @param targetTime 目标时间（非空）
     * @param duration   比较的时长（≥0）
     * @param unit       时间单位（非空，如ChronoUnit.SECONDS/HOURS）
     * @return true=差值等于duration，false=不等于
     */
    public static boolean equalNow(LocalDateTime targetTime, Integer duration, ChronoUnit unit) {
        return compareTimeDiff(targetTime, duration, unit, TimeDiffType.EQ);
    }

    /**
     * 通用时间差值比较函数（默认单位：分钟）
     * @param targetTime  目标时间（非空）
     * @param duration    比较的时长（≥0）
     * @param compareType 比较类型（枚举，非空）
     * @return true=符合比较条件，false=不符合
     * @throws IllegalArgumentException 参数不合法时抛出
     */
    private static boolean compareTimeDiff(LocalDateTime targetTime, Integer duration, TimeDiffType compareType) {
        return compareTimeDiff(targetTime, duration, DEFAULT_UNIT, compareType);
    }

    /**
     * 通用时间差值比较函数（指定时间单位）
     * @param targetTime  目标时间（非空）
     * @param duration    比较的时长（≥0）
     * @param unit        时间单位（非空）
     * @param compareType 比较类型（枚举，非空）
     * @return true=符合比较条件，false=不符合
     * @throws IllegalArgumentException 参数不合法时抛出
     */
    private static boolean compareTimeDiff(LocalDateTime targetTime, Integer duration,
                                           ChronoUnit unit, TimeDiffType compareType) {
        // 参数校验（EQ类型允许duration=0，其他类型duration≥0）
        validateParams(targetTime, duration, unit, compareType);

        // 统一转换为北京时间（避免时区差导致的计算错误）
        LocalDateTime now = LocalDateTime.now(zoneId);
        LocalDateTime targetInCST = convertToCst(targetTime);

        // 根据比较类型计算差值并判断
        long diff;
        return switch (compareType) {
            // GT：目标时间 - 当前时间 > duration → 对应afterNow
            case GT -> {
                diff = unit.between(now, targetInCST);
                yield diff > duration;
            }
            // LT：当前时间 - 目标时间 > duration → 对应beforeNow
            case LT -> {
                diff = unit.between(targetInCST, now);
                yield diff > duration;
            }
            // EQ：时间差值的绝对值 = duration → 对应equalNow
            case EQ -> {
                diff = Math.abs(unit.between(now, targetInCST));
                yield diff == duration;
            }
        };
    }

    /**
     * 参数合法性校验（差异化校验EQ类型）
     */
    private static void validateParams(LocalDateTime targetTime, Integer duration,
                                       ChronoUnit unit, TimeDiffType compareType) {
        // 目标时间非空
        if (targetTime == null) {
            throw new IllegalArgumentException("目标时间（targetTime）不能为空");
        }
        // 时间单位非空
        if (unit == null) {
            throw new IllegalArgumentException("时间单位（unit）不能为空，支持：SECONDS/MINUTES/HOURS/DAYS等");
        }
        // 比较类型非空
        if (compareType == null) {
            throw new IllegalArgumentException("比较类型（compareType）不能为空，需传入TimeDiffType枚举值");
        }
        // 时长校验：EQ类型允许=0，其他类型≥0（防止负数）
        if (duration == null || duration < 0) {
            throw new IllegalArgumentException("比较时长（duration）必须≥0，当前值：" + duration);
        }
    }

    /**
     * 时间偏移参数校验（时长≥0、单位非空）
     */
    private static void validateOffsetParams(Integer duration, ChronoUnit unit) {
        if (duration == null || duration < 0) {
            throw new IllegalArgumentException("偏移时长（duration）必须≥0，当前值：" + duration);
        }
        if (unit == null) {
            throw new IllegalArgumentException("时间单位（unit）不能为空，支持：SECONDS/MINUTES/HOURS/DAYS等");
        }
    }

    /**
     * 将任意时区的LocalDateTime转换为北京时间
     */
    private static LocalDateTime convertToCst(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(zoneId)
                .toLocalDateTime();
    }
}