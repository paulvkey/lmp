package com.xjtu.springboot.component;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 流式响应内容暂存器，用于累积分片结果
 */
@Component
public class MessageHolder {
    // 存储会话ID与对应累积内容的映射（线程安全）
    private final ConcurrentHashMap<Long, AtomicReference<StringBuilder>> contentMap = new ConcurrentHashMap<>();
    // 存储会话创建时间，用于超时清理
    private final ConcurrentHashMap<Long, Long> sessionCreateTime = new ConcurrentHashMap<>();
    // 锁对象用于定时任务安全操作
    private final ReentrantLock lock = new ReentrantLock();

    private static final Logger log = LoggerFactory.getLogger(MessageHolder.class);

    /**
     * 初始化会话的内容容器
     */
    public void initContentHolder(Long sessionId) {
        contentMap.putIfAbsent(sessionId, new AtomicReference<>(new StringBuilder()));
        sessionCreateTime.put(sessionId, System.currentTimeMillis());
        log.debug("初始化会话[{}]的流式内容容器", sessionId);

    }

    /**
     * 累加流式内容
     */
    public void appendContent(Long sessionId, String content) {
        if (sessionId == null || StringUtils.isBlank(content)) return;

        AtomicReference<StringBuilder> contentRef = contentMap.get(sessionId);
        if (contentRef != null) {
            // 限制单会话最大内容长度（防止内存溢出）
            StringBuilder sb = contentRef.get();
            // 5MB限制
            if (sb.length() + content.length() > 1024 * 1024 * 5) {
                log.warn("会话[{}]内容超过5MB，将截断", sessionId);
                return;
            }
            sb.append(content);
            sessionCreateTime.put(sessionId, System.currentTimeMillis());
        }
    }

    /**
     * 获取完整内容并清除暂存
     */
    public String getCompleteContent(Long sessionId) {
        lock.lock();
        try {
            AtomicReference<StringBuilder> contentRef = contentMap.remove(sessionId);
            sessionCreateTime.remove(sessionId);
            return contentRef != null ? contentRef.get().toString() : "";
        } finally {
            lock.unlock();
        }
    }

    /**
     * 清除会话的暂存内容（异常时使用）
     */
    public void clearContent(Long sessionId) {
        lock.lock();
        try {
            contentMap.remove(sessionId);
            sessionCreateTime.remove(sessionId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 定时清理超时会话（每30分钟执行一次）
     * 清理超过1小时未活跃的会话
     */
    @Scheduled(fixedRate = 1800000) // 30分钟 = 1800000毫秒
    public void cleanTimeoutSessions() {
        lock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            long timeout = 3600000; // 1小时超时
            for (Map.Entry<Long, Long> entry : sessionCreateTime.entrySet()) {
                if (currentTime - entry.getValue() > timeout) {
                    Long sessionId = entry.getKey();
                    contentMap.remove(sessionId);
                    sessionCreateTime.remove(sessionId);
                }
            }
        } finally {
            lock.unlock();
        }
    }
}