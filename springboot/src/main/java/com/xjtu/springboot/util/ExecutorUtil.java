package com.xjtu.springboot.util;

import jakarta.annotation.PreDestroy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ExecutorUtil {
    public static final ExecutorService SseExecutor = Executors.newFixedThreadPool(10, r -> {
        Thread t = new Thread(r);
        t.setName("sse-worker-" + t.getId());
        t.setDaemon(true);
        return t;
    });

    // 应用关闭时关闭线程池（防止内存泄漏）
    @PreDestroy
    public void destroy() {
        SseExecutor.shutdown();
        try {
            if (!SseExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                SseExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            SseExecutor.shutdownNow();
        }
    }
}
