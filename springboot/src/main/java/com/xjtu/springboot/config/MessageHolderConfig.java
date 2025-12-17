package com.xjtu.springboot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "message.holder")
public class MessageHolderConfig {
    /** 全局默认会话超时时间（毫秒） */
    private long defaultSessionTimeout = 3600000L;
    /** 单会话最大内容长度（字节） */
    private int maxContentLength = 1024 * 1024 * 5;
    /** 定时清理频率（毫秒） */
    private long cleanFixedRate = 1800000L;
    /** 定时清理分批大小 */
    private int cleanBatchSize = 100;
    /** 是否启用软引用回收 */
    private boolean enableSoftReference = true;
    /** StringBuffer 池配置 */
    private int bufferPoolSize = 1000;
    private int bufferInitialCapacity = 1024;
}