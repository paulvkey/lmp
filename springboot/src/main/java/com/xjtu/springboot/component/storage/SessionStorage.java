package com.xjtu.springboot.component.storage;

/**
 * 会话存储策略接口（仅本地实现，便于后续扩展）
 */
public interface SessionStorage {
    /**
     * 初始化会话
     */
    void init(Long userId, Long sessionId);

    /**
     * 累加内容
     */
    boolean appendContent(Long userId, Long sessionId, String content, Boolean isThinking);

    /**
     * 获取内容（按需清除）
     */
    String getContent(Long userId, Long sessionId, Boolean isThinking);

    /**
     * 清除指定会话
     */
    void clear(Long userId, Long sessionId);

    /**
     * 批量清除用户所有会话
     */
    void clearAll(Long userId);

    /**
     * 清理超时会话
     */
    void cleanTimeoutSessions();
}
