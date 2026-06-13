package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import io.github.lvdaxianer.doclens.j.processing.application.DocumentPageTaskExecutionService;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentPageTaskRecoveryService;
import java.time.OffsetDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 页任务 OCR worker 周期调度器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
public class PageTaskWorkerScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageTaskWorkerScheduler.class);

    private final DocumentPageTaskExecutionService executionService;
    private final DocumentPageTaskRecoveryService recoveryService;
    private final ScheduledExecutorService schedulerExecutor;
    private final int intervalMillis;
    private final int recoveryLimit;
    private final AtomicBoolean started = new AtomicBoolean();

    /**
     * 创建页任务 OCR worker 周期调度器。
     *
     * @param dependencies 调度依赖
     * @param intervalMillis 调度间隔毫秒
     * @param recoveryLimit 每轮最大恢复数量
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    public PageTaskWorkerScheduler(
            PageTaskWorkerSchedulerDependencies dependencies,
            int intervalMillis,
            int recoveryLimit
    ) {
        this.executionService = dependencies.executionService();
        this.recoveryService = dependencies.recoveryService();
        this.schedulerExecutor = dependencies.schedulerExecutor();
        this.intervalMillis = Math.max(1, intervalMillis);
        this.recoveryLimit = Math.max(1, recoveryLimit);
    }

    /**
     * 启动页任务 OCR worker 调度。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    public void start() {
        if (started.compareAndSet(false, true)) {
            LOGGER.info("[页任务OCR] 启动页任务调度, intervalMillis={}", intervalMillis);
            scheduleNext(0);
        } else {
            LOGGER.info("[页任务OCR] 页任务调度已启动, intervalMillis={}", intervalMillis);
        }
    }

    /**
     * 安全执行一轮页任务扫描。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void runSafely() {
        try {
            // 先恢复过期 PROCESSING 页任务，再抢占 QUEUED 页任务。
            // 这样应用重启后，遗留锁不会长期占住 OCR 并发槽位。
            int recoveredCount = recoveryService.recoverExpiredTasks(OffsetDateTime.now(), recoveryLimit);
            int claimedCount = executionService.runOnce();
            LOGGER.info("[页任务OCR] 页任务扫描完成, recoveredCount={}, claimedCount={}", recoveredCount,
                    claimedCount);
        } catch (RuntimeException ex) {
            LOGGER.warn("[页任务OCR] 页任务扫描失败, error={}", ex.getMessage(), ex);
        } finally {
            rescheduleIfRunning();
        }
    }

    /**
     * 在调度器仍运行时安排下一轮。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void rescheduleIfRunning() {
        if (started.get()) {
            scheduleNext(intervalMillis);
        } else {
            // 调度器未启动时不继续排队，避免重复注册任务。
        }
    }

    /**
     * 安排下一轮页任务扫描。
     *
     * @param delayMillis 延迟毫秒数
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void scheduleNext(int delayMillis) {
        if (!schedulerExecutor.isShutdown()) {
            schedulerExecutor.schedule(this::runSafely, Math.max(0, delayMillis), TimeUnit.MILLISECONDS);
        } else {
            // 调度线程池销毁后不再继续排新任务。
        }
    }

    /**
     * 页任务 worker 调度器依赖集合。
     *
     * @param executionService 页任务执行服务
     * @param recoveryService 页任务恢复服务
     * @param schedulerExecutor 调度线程池
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    public record PageTaskWorkerSchedulerDependencies(
            DocumentPageTaskExecutionService executionService,
            DocumentPageTaskRecoveryService recoveryService,
            ScheduledExecutorService schedulerExecutor
    ) {
    }
}
