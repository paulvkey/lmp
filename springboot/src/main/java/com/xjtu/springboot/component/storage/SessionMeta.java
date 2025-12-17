package com.xjtu.springboot.component.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import com.xjtu.springboot.component.pool.StringBufferPool;

/**
 * 会话元数据（封装内容、时间、锁、池化缓冲区）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionMeta {
    /** 思考内容（软引用，内存紧张时回收） */
    private SoftReference<StringBuffer> thinkingContent;
    /** 回复内容（软引用，内存紧张时回收） */
    private SoftReference<StringBuffer> replyContent;
    /** 会话创建时间（毫秒） */
    private long createTime;
    /** 最后活跃时间（毫秒，原子类保证线程安全） */
    private AtomicLong lastActiveTime;
    /** 会话超时时间（毫秒） */
    private long timeout;
    /** 会话专属锁（细粒度锁，避免全局阻塞） */
    private ReentrantLock sessionLock;
    /** 缓冲区池（复用缓冲区） */
    private transient StringBufferPool bufferPool;

    /**
     * 初始化会话元数据
     */
    public static SessionMeta init(long defaultTimeout, StringBufferPool bufferPool) {
        SessionMeta meta = new SessionMeta();
        // 从池获取缓冲区
        meta.setThinkingContent(new SoftReference<>(bufferPool.borrow()));
        meta.setReplyContent(new SoftReference<>(bufferPool.borrow()));
        meta.setCreateTime(System.currentTimeMillis());
        meta.setLastActiveTime(new AtomicLong(System.currentTimeMillis()));
        meta.setTimeout(defaultTimeout);
        meta.setSessionLock(new ReentrantLock());
        meta.setBufferPool(bufferPool);
        return meta;
    }

    /**
     * 获取思考内容缓冲区（空则从池重新获取）
     */
    public StringBuffer getThinkingContent() {
        StringBuffer sb = thinkingContent.get();
        if (sb == null) {
            sb = bufferPool.borrow();
            thinkingContent = new SoftReference<>(sb);
        }
        return sb;
    }

    /**
     * 获取回复内容缓冲区（空则从池重新获取）
     */
    public StringBuffer getReplyContent() {
        StringBuffer sb = replyContent.get();
        if (sb == null) {
            sb = bufferPool.borrow();
            replyContent = new SoftReference<>(sb);
        }
        return sb;
    }

    /**
     * 更新最后活跃时间
     */
    public void updateLastActiveTime() {
        lastActiveTime.set(System.currentTimeMillis());
    }

    /**
     * 释放缓冲区（归还到池）
     */
    public void releaseBuffer() {
        if (thinkingContent.get() != null) {
            bufferPool.recycle(thinkingContent.get());
            thinkingContent.clear();
        }
        if (replyContent.get() != null) {
            bufferPool.recycle(replyContent.get());
            replyContent.clear();
        }
    }
}