package com.xjtu.springboot.component.pool;

import com.xjtu.springboot.config.MessageHolderConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Slf4j
@Component
public class StringBufferPool {
    private final MessageHolderConfig config;
    // 阻塞队列实现池（线程安全）
    private BlockingQueue<StringBuffer> pool;

    public StringBufferPool(MessageHolderConfig config) {
        this.config = config;
    }

    /**
     * 初始化池（预创建缓冲区）
     */
    @PostConstruct
    public void initPool() {
        int poolSize = config.getBufferPoolSize();
        int initialCapacity = config.getBufferInitialCapacity();
        this.pool = new ArrayBlockingQueue<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            pool.offer(new StringBuffer(initialCapacity));
        }
        log.info("StringBuffer 池初始化完成，池大小：{}，初始容量：{}", poolSize, initialCapacity);
    }

    /**
     * 从池获取缓冲区（无可用则创建新的）
     */
    public StringBuffer borrow() {
        StringBuffer sb = pool.poll();
        if (sb == null) {
            // 池空时创建新的（带初始容量）
            return new StringBuffer(config.getBufferInitialCapacity());
        }
        // 清空内容，复用缓冲区
        sb.setLength(0);
        return sb;
    }

    /**
     * 归还缓冲区到池（池满则丢弃，避免内存浪费）
     */
    public void recycle(StringBuffer sb) {
        if (sb == null) {
            return;
        }
        // 超过2倍初始容量的缓冲区直接丢弃（避免大对象占用池）
        if (sb.capacity() > config.getBufferInitialCapacity() * 2) {
            return;
        }
        sb.setLength(0); // 清空内容
        if (!pool.offer(sb)) {
            // 池满，丢弃（非核心，不抛异常）
            log.warn("StringBuffer 池已满，丢弃缓冲区（容量：{}）", sb.capacity());
        }
    }
}