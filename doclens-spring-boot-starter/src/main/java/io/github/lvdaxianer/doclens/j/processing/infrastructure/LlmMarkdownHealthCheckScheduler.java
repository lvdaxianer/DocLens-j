package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LLM Markdown 健康检查周期调度器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public class LlmMarkdownHealthCheckScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LlmMarkdownHealthCheckScheduler.class);

    private final LlmMarkdownHealthChecker healthChecker;
    private final ScheduledExecutorService schedulerExecutor;
    private final ExecutorService healthExecutor;
    private final int intervalSeconds;
    private final AtomicBoolean started = new AtomicBoolean();

    /**
     * 创建 LLM Markdown 健康检查周期调度器。
     *
     * @param healthChecker LLM Markdown 健康检查器
     * @param schedulerExecutor 调度线程池
     * @param healthExecutor 探活工作线程池
     * @param intervalSeconds 探测间隔秒数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public LlmMarkdownHealthCheckScheduler(
            LlmMarkdownHealthChecker healthChecker,
            ScheduledExecutorService schedulerExecutor,
            ExecutorService healthExecutor,
            int intervalSeconds
    ) {
        this.healthChecker = healthChecker;
        this.schedulerExecutor = schedulerExecutor;
        this.healthExecutor = healthExecutor;
        this.intervalSeconds = Math.max(1, intervalSeconds);
    }

    /**
     * 启动周期探活。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public void start() {
        if (started.compareAndSet(false, true)) {
            LOGGER.info("[LLM健康检查] 启动周期健康检查调度, intervalSeconds={}", intervalSeconds);
            scheduleNext(0);
        } else {
            LOGGER.info("[LLM健康检查] 周期健康检查调度已启动, intervalSeconds={}", intervalSeconds);
        }
    }

    /**
     * 提交单次健康检查到独立工作线程池。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void runSafely() {
        try {
            healthExecutor.submit(this::checkSafely);
        } catch (RuntimeException ex) {
            LOGGER.warn("[LLM健康检查] 提交周期健康检查失败, error={}", ex.getMessage(), ex);
        } finally {
            if (started.get()) {
                scheduleNext(intervalSeconds);
            } else {
                // 调度尚未启动时无需继续排下一轮任务。
            }
        }
    }

    /**
     * 安全执行单次健康检查。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void checkSafely() {
        try {
            healthChecker.checkOnce();
        } catch (RuntimeException ex) {
            LOGGER.warn("[LLM健康检查] 周期健康检查失败, error={}", ex.getMessage(), ex);
        }
    }

    /**
     * 安排下一轮检查。
     *
     * @param delaySeconds 延迟秒数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void scheduleNext(int delaySeconds) {
        if (!schedulerExecutor.isShutdown()) {
            schedulerExecutor.schedule(this::runSafely, Math.max(0, delaySeconds), TimeUnit.SECONDS);
        } else {
            // 调度器关闭后不再排新任务，避免销毁阶段抛出拒绝执行异常。
        }
    }
}
