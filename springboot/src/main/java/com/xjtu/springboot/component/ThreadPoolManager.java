package com.xjtu.springboot.component;

import com.xjtu.springboot.config.ThreadPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ThreadPoolManager {
    private static final Logger log = LoggerFactory.getLogger(ThreadPoolManager.class);

    // 注入配置类
    @Autowired
    private ThreadPoolConfig threadPoolConfig;

    /**
     * 存储已创建的线程池：key=业务标识（如sse/order），value=线程池实例
     */
    private final Map<String, ExecutorService> threadPoolMap = new ConcurrentHashMap<>();

    /**
     * 线程池关闭标记：key=业务标识，value=是否已关闭
     */
    private final Map<String, AtomicBoolean> shutdownFlagMap = new ConcurrentHashMap<>();

    /**
     * 获取指定业务的线程池（懒加载创建）
     * @param bizName 业务标识（如"sse"、"order"、"message"）
     * @return 对应业务的线程池
     */
    public ExecutorService getThreadPool(String bizName) {
        // 双重检查锁：保证线程安全且懒加载
        if (!threadPoolMap.containsKey(bizName)) {
            synchronized (this) {
                if (!threadPoolMap.containsKey(bizName)) {
                    ExecutorService pool = createThreadPool(bizName);
                    threadPoolMap.put(bizName, pool);
                    shutdownFlagMap.put(bizName, new AtomicBoolean(false));
                    log.debug("通用线程池[{}]创建完成，配置：核心数={}，线程名前缀={}",
                            bizName,
                            threadPoolConfig.getConfig(bizName).getCoreSize(),
                            threadPoolConfig.getConfig(bizName).getThreadNamePrefix());
                }
            }
        }

        // 检查线程池是否已关闭
        AtomicBoolean isShutdown = shutdownFlagMap.get(bizName);
        if (isShutdown.get()) {
            log.error("线程池[{}]已关闭，拒绝获取", bizName);
            throw new IllegalStateException("线程池[" + bizName + "]已关闭，无法使用");
        }

        return threadPoolMap.get(bizName);
    }

    /**
     * 手动关闭指定业务的线程池
     * @param bizName 业务标识
     */
    public void shutdownThreadPool(String bizName) {
        if (!threadPoolMap.containsKey(bizName)) {
            log.warn("线程池[{}]不存在，无需关闭", bizName);
            return;
        }

        AtomicBoolean isShutdown = shutdownFlagMap.get(bizName);
        // 原子操作：确保仅关闭一次
        if (isShutdown.compareAndSet(false, true)) {
            ExecutorService pool = threadPoolMap.get(bizName);
            ThreadPoolConfig.PoolConfig config = threadPoolConfig.getConfig(bizName);
            log.debug("开始关闭线程池[{}]，等待现有任务完成（超时时间：{}秒）", bizName, config.getShutdownTimeout());

            pool.shutdown();
            try {
                // 等待超时时间，未关闭则强制终止
                if (!pool.awaitTermination(config.getShutdownTimeout(), TimeUnit.SECONDS)) {
                    log.warn("线程池[{}]未在{}秒内正常关闭，强制终止剩余任务", bizName, config.getShutdownTimeout());
                    pool.shutdownNow();

                    // 再次等待，确保强制关闭生效
                    if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
                        log.error("线程池[{}]强制关闭失败，可能存在资源泄漏", bizName);
                    }
                }
                log.debug("线程池[{}]已成功关闭", bizName);
            } catch (InterruptedException e) {
                // 恢复中断状态，符合并发最佳实践
                Thread.currentThread().interrupt();
                log.error("线程池[{}]关闭过程被中断，强制终止", bizName, e);
                pool.shutdownNow();
            }
        } else {
            log.debug("线程池[{}]已关闭，无需重复操作", bizName);
        }
    }

    /**
     * 创建指定业务的线程池（内部方法）
     */
    private ExecutorService createThreadPool(String bizName) {
        ThreadPoolConfig.PoolConfig config = threadPoolConfig.getConfig(bizName);
        return Executors.newFixedThreadPool(
                config.getCoreSize(),
                createThreadFactory(config.getThreadNamePrefix())
        );
    }

    /**
     * 创建通用线程工厂（自定义线程名、守护线程、异常处理器）
     */
    private ThreadFactory createThreadFactory(String threadNamePrefix) {
        return runnable -> {
            Thread thread = new Thread(runnable);
            // 线程名格式：前缀-线程ID
            thread.setName(threadNamePrefix + "-worker-" + thread.getId());
            // 守护线程：JVM退出时自动销毁
            thread.setDaemon(true);
            // 未捕获异常处理器：防止线程静默失败
            thread.setUncaughtExceptionHandler((t, e) ->
                    log.error("线程池线程[{}]执行异常", t.getName(), e));
            return thread;
        };
    }

    /**
     * Spring容器销毁时，关闭所有线程池
     */
    @PreDestroy
    public void destroyAll() {
        // 遍历关闭所有线程池
        threadPoolMap.keySet().forEach(this::shutdownThreadPool);
        log.debug("所有线程池关闭完成");
    }
}