package com.xjtu.springboot.component.storage;

import com.xjtu.springboot.config.MessageHolderConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MessageHolder {
    // 存储策略（本地实现）
    private final SessionStorage sessionStorage;
    // 配置项
    private final MessageHolderConfig properties;
    // 定时任务专用线程池（隔离核心业务）
    private final ScheduledExecutorService scheduledExecutor;

    public MessageHolder(SessionStorage sessionStorage,
                         MessageHolderConfig properties) {
        this.sessionStorage = sessionStorage;
        this.properties = properties;

        // 初始化定时任务线程池（守护线程 + 低优先级）
        this.scheduledExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "message-holder-scheduler");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
    }

    /**
     * 初始化：启动异步清理任务
     */
    @PostConstruct
    public void init() {
        log.debug("MessageHolder 初始化完成，配置：{}", properties);
        // 异步定时清理（避免占用主线程）
        scheduledExecutor.scheduleAtFixedRate(
                sessionStorage::cleanTimeoutSessions,
                properties.getCleanFixedRate(),
                properties.getCleanFixedRate(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 初始化用户+会话的内容容器
     */
    public void initHolder(Long userId, Long sessionId) {
        sessionStorage.init(userId, sessionId);
    }

    /**
     * 累加流式内容（线程安全）
     */
    public boolean appendContent(Long userId, Long sessionId, String content, Boolean isThinking) {
        return sessionStorage.appendContent(userId, sessionId, content, isThinking);
    }

    /**
     * 获取完整内容并按需清除
     */
    public String getCompleteContent(Long userId, Long sessionId, Boolean isThinking) {
        return sessionStorage.getContent(userId, sessionId, isThinking);
    }

    /**
     * 清除指定用户+会话的暂存内容
     */
    public void clearContent(Long userId, Long sessionId) {
        sessionStorage.clear(userId, sessionId);
    }

    /**
     * 批量清除指定用户的所有会话内容
     */
    public void clearAllByUserId(Long userId) {
        sessionStorage.clearAll(userId);
    }

    // ======== 监控指标暴露（供外部接口/监控调用） ========
    public int getActiveUserCount() {
        if (sessionStorage instanceof LocalSession) {
            return ((LocalSession) sessionStorage).getActiveUserCount();
        }
        return 0;
    }

    public int getActiveSessionCount() {
        if (sessionStorage instanceof LocalSession) {
            return ((LocalSession) sessionStorage).getActiveSessionCount();
        }
        return 0;
    }
}