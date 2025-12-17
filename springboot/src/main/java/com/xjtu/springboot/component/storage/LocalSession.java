package com.xjtu.springboot.component.storage;

import com.xjtu.springboot.component.pool.StringBufferPool;
import com.xjtu.springboot.config.MessageHolderConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class LocalSession implements SessionStorage {
    // userId → sessionId → SessionMeta
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, SessionMeta>> contentMap = new ConcurrentHashMap<>();
    // 配置项
    private final MessageHolderConfig properties;
    // 缓冲区池
    private final StringBufferPool bufferPool;
    // 监控指标
    private final Counter appendSuccessCounter;
    private final Counter appendFailCounter;
    private final Counter cleanTimeoutCounter;
    private final Timer appendTimer;
    // 活跃计数（原子类保证线程安全）
    private final AtomicInteger activeUserCount = new AtomicInteger(0);
    private final AtomicInteger activeSessionCount = new AtomicInteger(0);

    public LocalSession(MessageHolderConfig properties,
                        StringBufferPool bufferPool,
                        MeterRegistry meterRegistry) {
        this.properties = properties;
        this.bufferPool = bufferPool;

        // 初始化监控指标
        this.appendSuccessCounter = meterRegistry.counter("message.holder.append.success");
        this.appendFailCounter = meterRegistry.counter("message.holder.append.fail");
        this.cleanTimeoutCounter = meterRegistry.counter("message.holder.clean.timeout");
        this.appendTimer = meterRegistry.timer("message.holder.append.duration");
    }

    @Override
    public void init(Long userId, Long sessionId) {
        if (userId == null || sessionId == null) {
            log.warn("初始化会话失败：userId/sessionId 为空");
            return;
        }

        // 原子化创建用户维度 Map
        contentMap.computeIfAbsent(userId, k -> {
            activeUserCount.incrementAndGet();
            return new ConcurrentHashMap<>();
        });

        // 原子化创建会话元数据
        ConcurrentHashMap<Long, SessionMeta> userSessionMap = contentMap.get(userId);
        userSessionMap.computeIfAbsent(sessionId, k -> {
            activeSessionCount.incrementAndGet();
            return SessionMeta.init(properties.getDefaultSessionTimeout(), bufferPool);
        });

        log.debug("初始化用户[{}]会话[{}]（超时时间：{}ms）", userId, sessionId, properties.getDefaultSessionTimeout());
    }

    @Override
    public boolean appendContent(Long userId, Long sessionId, String content, Boolean isThinking) {
        // 参数校验
        if (userId == null || sessionId == null || content == null || isThinking == null) {
            appendFailCounter.increment();
            log.warn("累加内容失败：参数为空（userId={}, sessionId={}, content={}, isThinking={})",
                    userId, sessionId, content, isThinking);
            return false;
        }

        // 耗时监控
        return appendTimer.record(() -> {
            // 获取用户+会话元数据
            ConcurrentHashMap<Long, SessionMeta> userSessionMap = contentMap.get(userId);
            if (userSessionMap == null) {
                appendFailCounter.increment();
                log.warn("累加内容失败：用户[{}]未初始化", userId);
                return false;
            }
            SessionMeta sessionMeta = userSessionMap.get(sessionId);
            if (sessionMeta == null) {
                appendFailCounter.increment();
                log.warn("累加内容失败：用户[{}]会话[{}]未初始化", userId, sessionId);
                return false;
            }

            // 细粒度锁：仅锁定当前会话
            sessionMeta.getSessionLock().lock();
            try {
                // 获取目标缓冲区
                StringBuffer targetSb = isThinking ? sessionMeta.getThinkingContent() : sessionMeta.getReplyContent();

                // 长度限制校验
                int newLength = targetSb.length() + content.length();
                if (newLength > properties.getMaxContentLength()) {
                    appendFailCounter.increment();
                    log.warn("用户[{}]会话[{}]内容超限（当前：{}，最大：{}）",
                            userId, sessionId, newLength, properties.getMaxContentLength());
                    return false;
                }

                // 累加内容 + 更新活跃时间
                targetSb.append(content);
                sessionMeta.updateLastActiveTime();
                appendSuccessCounter.increment();

                log.trace("用户[{}]会话[{}]累加{}内容，当前长度：{}",
                        userId, sessionId, isThinking ? "思考" : "回复", targetSb.length());
                return true;
            } finally {
                sessionMeta.getSessionLock().unlock();
            }
        });
    }

    @Override
    public String getContent(Long userId, Long sessionId, Boolean isThinking) {
        if (userId == null || sessionId == null || isThinking == null) {
            return "";
        }

        // 获取用户+会话元数据
        ConcurrentHashMap<Long, SessionMeta> userSessionMap = contentMap.get(userId);
        if (userSessionMap == null) {
            return "";
        }
        SessionMeta sessionMeta = userSessionMap.get(sessionId);
        if (sessionMeta == null) {
            return "";
        }

        // 细粒度锁保证原子性
        sessionMeta.getSessionLock().lock();
        try {
            StringBuffer targetSb = isThinking ? sessionMeta.getThinkingContent() : sessionMeta.getReplyContent();
            String result = targetSb.toString();

            // 非思考内容：清除会话 + 释放缓冲区
            if (!isThinking) {
                // 释放缓冲区到池
                sessionMeta.releaseBuffer();
                // 移除会话
                userSessionMap.remove(sessionId);
                activeSessionCount.decrementAndGet();
                // 无会话时清理用户维度 Map
                if (userSessionMap.isEmpty()) {
                    contentMap.remove(userId);
                    activeUserCount.decrementAndGet();
                }
                log.debug("用户[{}]会话[{}]获取回复内容并清除，长度：{}", userId, sessionId, result.length());
            } else {
                log.debug("用户[{}]会话[{}]获取思考内容，长度：{}", userId, sessionId, result.length());
            }
            return result;
        } finally {
            sessionMeta.getSessionLock().unlock();
        }
    }

    @Override
    public void clear(Long userId, Long sessionId) {
        if (userId == null || sessionId == null) {
            return;
        }

        // 获取用户会话 Map
        ConcurrentHashMap<Long, SessionMeta> userSessionMap = contentMap.get(userId);
        if (userSessionMap == null) {
            return;
        }

        // 移除并释放会话缓冲区
        SessionMeta sessionMeta = userSessionMap.remove(sessionId);
        if (sessionMeta != null) {
            sessionMeta.releaseBuffer();
            activeSessionCount.decrementAndGet();
            cleanTimeoutCounter.increment();
            // 清理空用户 Map
            if (userSessionMap.isEmpty()) {
                contentMap.remove(userId);
                activeUserCount.decrementAndGet();
            }
            log.debug("清除用户[{}]会话[{}]，当前活跃用户：{}，活跃会话：{}",
                    userId, sessionId, activeUserCount.get(), activeSessionCount.get());
        }
    }

    @Override
    public void clearAll(Long userId) {
        if (userId == null) {
            return;
        }

        // 移除用户所有会话
        ConcurrentHashMap<Long, SessionMeta> userSessionMap = contentMap.remove(userId);
        if (userSessionMap != null) {
            // 批量释放缓冲区
            userSessionMap.values().forEach(SessionMeta::releaseBuffer);
            // 更新计数
            cleanTimeoutCounter.increment(userSessionMap.size());
            activeSessionCount.addAndGet(-userSessionMap.size());
            activeUserCount.decrementAndGet();

            log.debug("批量清除用户[{}]的{}个会话，当前活跃用户：{}，活跃会话：{}",
                    userId, userSessionMap.size(), activeUserCount.get(), activeSessionCount.get());
        }
    }

    @Override
    public void cleanTimeoutSessions() {
        long currentTime = System.currentTimeMillis();
        int batchCount = 0;
        long cleanedCount = 0;

        // 分批遍历用户（避免全量遍历阻塞）
        Iterator<Map.Entry<Long, ConcurrentHashMap<Long, SessionMeta>>> userIterator = contentMap.entrySet().iterator();
        while (userIterator.hasNext() && batchCount < properties.getCleanBatchSize()) {
            Map.Entry<Long, ConcurrentHashMap<Long, SessionMeta>> userEntry = userIterator.next();
            Long userId = userEntry.getKey();
            ConcurrentHashMap<Long, SessionMeta> userSessionMap = userEntry.getValue();

            // 遍历用户下的会话
            Iterator<Map.Entry<Long, SessionMeta>> sessionIterator = userSessionMap.entrySet().iterator();
            while (sessionIterator.hasNext()) {
                Map.Entry<Long, SessionMeta> sessionEntry = sessionIterator.next();
                Long sessionId = sessionEntry.getKey();
                SessionMeta sessionMeta = sessionEntry.getValue();

                // 按最后活跃时间判断超时
                if (currentTime - sessionMeta.getLastActiveTime().get() > sessionMeta.getTimeout()) {
                    // 释放缓冲区 + 移除会话
                    sessionMeta.releaseBuffer();
                    sessionIterator.remove();
                    activeSessionCount.decrementAndGet();
                    cleanedCount++;
                    log.trace("清理超时会话：用户[{}]会话[{}]（最后活跃：{}）",
                            userId, sessionId, sessionMeta.getLastActiveTime().get());
                }
            }

            // 清理空用户 Map
            if (userSessionMap.isEmpty()) {
                userIterator.remove();
                activeUserCount.decrementAndGet();
            }
            batchCount++;
        }

        // 更新监控指标 + 日志
        if (cleanedCount > 0) {
            cleanTimeoutCounter.increment(cleanedCount);
            log.info("定时清理超时会话完成：本次清理{}个，当前活跃用户：{}，活跃会话：{}",
                    cleanedCount, activeUserCount.get(), activeSessionCount.get());
        }
    }

    // ======== 监控指标暴露 ========
    public int getActiveUserCount() {
        return activeUserCount.get();
    }

    public int getActiveSessionCount() {
        return activeSessionCount.get();
    }
}