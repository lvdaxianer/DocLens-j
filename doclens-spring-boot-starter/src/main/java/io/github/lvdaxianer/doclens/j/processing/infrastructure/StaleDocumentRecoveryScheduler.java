package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import io.github.lvdaxianer.doclens.j.processing.application.StaleDocumentRecoveryService;
import java.time.OffsetDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 卡死文档恢复周期调度器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public class StaleDocumentRecoveryScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaleDocumentRecoveryScheduler.class);

    private final StaleDocumentRecoveryService recoveryService;
    private final ScheduledExecutorService schedulerExecutor;
    private final int intervalSeconds;
    private final AtomicBoolean started = new AtomicBoolean();

    /**
     * 创建卡死文档恢复调度器。
     *
     * @param recoveryService 卡死文档恢复服务
     * @param schedulerExecutor 调度线程池
     * @param intervalSeconds 调度间隔秒数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public StaleDocumentRecoveryScheduler(
            StaleDocumentRecoveryService recoveryService,
            ScheduledExecutorService schedulerExecutor,
            int intervalSeconds
    ) {
        this.recoveryService = recoveryService;
        this.schedulerExecutor = schedulerExecutor;
        this.intervalSeconds = Math.max(1, intervalSeconds);
    }

    /**
     * 启动卡死文档恢复调度。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public void start() {
        if (started.compareAndSet(false, true)) {
            LOGGER.info("[文档恢复] 启动卡死文档恢复调度, intervalSeconds={}", intervalSeconds);
            scheduleNext(0);
        } else {
            LOGGER.info("[文档恢复] 卡死文档恢复调度已启动, intervalSeconds={}", intervalSeconds);
        }
    }

    /**
     * 安全执行一轮 stale 扫描并安排下一轮。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void runSafely() {
        try {
            int recovered = recoveryService.markStaleDocuments(OffsetDateTime.now());
            LOGGER.info("[文档恢复] 周期扫描完成, stalledCount={}", recovered);
        } catch (RuntimeException ex) {
            LOGGER.warn("[文档恢复] 周期扫描失败, error={}", ex.getMessage(), ex);
        } finally {
            if (started.get()) {
                scheduleNext(intervalSeconds);
            } else {
                // 调度器未启动时不继续排队，避免重复注册任务。
            }
        }
    }

    /**
     * 安排下一轮卡死扫描。
     *
     * @param delaySeconds 延迟秒数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void scheduleNext(int delaySeconds) {
        if (!schedulerExecutor.isShutdown()) {
            schedulerExecutor.schedule(this::runSafely, Math.max(0, delaySeconds), TimeUnit.SECONDS);
        } else {
            // 调度线程池销毁后不再继续排新任务。
        }
    }
}
