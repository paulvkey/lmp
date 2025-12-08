package com.xjtu.springboot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用线程池配置类
 * 从application.yml读取不同业务的线程池配置
 */
@Component
@ConfigurationProperties(prefix = "common.thread.pool")
public class ThreadPoolConfig {
    /**
     * 业务线程池配置映射：key=业务标识（如sse/order/message），value=具体配置
     */
    private Map<String, PoolConfig> configs = new HashMap<>();

    // 默认配置（未配置时使用）
    private static final PoolConfig DEFAULT_CONFIG = new PoolConfig(
            10,          // 默认核心线程数
            5,           // 默认关闭超时时间（秒）
            "default"    // 默认线程名前缀
    );

    /**
     * 获取指定业务的线程池配置，无配置则返回默认
     */
    public PoolConfig getConfig(String bizName) {
        return configs.getOrDefault(bizName, DEFAULT_CONFIG);
    }

    // 内部类：单线程池配置项
    public static class PoolConfig {
        private int coreSize;        // 核心线程数
        private int shutdownTimeout; // 关闭超时时间（秒）
        private String threadNamePrefix; // 线程名前缀

        // 空构造（Spring配置绑定需要）
        public PoolConfig() {
        }

        // 全参构造
        public PoolConfig(int coreSize, int shutdownTimeout, String threadNamePrefix) {
            this.coreSize = coreSize;
            this.shutdownTimeout = shutdownTimeout;
            this.threadNamePrefix = threadNamePrefix;
        }

        // getter/setter
        public int getCoreSize() {
            return coreSize;
        }

        public void setCoreSize(int coreSize) {
            this.coreSize = coreSize;
        }

        public int getShutdownTimeout() {
            return shutdownTimeout;
        }

        public void setShutdownTimeout(int shutdownTimeout) {
            this.shutdownTimeout = shutdownTimeout;
        }

        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }

        public void setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }
    }

    // getter/setter
    public Map<String, PoolConfig> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<String, PoolConfig> configs) {
        this.configs = configs;
    }
}
