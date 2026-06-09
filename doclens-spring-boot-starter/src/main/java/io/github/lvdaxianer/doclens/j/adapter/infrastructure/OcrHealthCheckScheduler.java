package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OCR 健康检查周期调度器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public class OcrHealthCheckScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcrHealthCheckScheduler.class);

    private final OcrHealthChecker healthChecker;
    private final OcrRuntimeNodePool nodePool;
    private final ScheduledExecutorService schedulerExecutor;
    private final int intervalSeconds;
    private final AtomicBoolean started = new AtomicBoolean();

    /**
     * 创建 OCR 健康检查周期调度器。
     *
     * @param healthChecker OCR 健康检查器
     * @param nodePool OCR 运行时节点池
     * @param schedulerExecutor 健康检查调度线程池
     * @param intervalSeconds 调度间隔秒数
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrHealthCheckScheduler(
            OcrHealthChecker healthChecker,
            OcrRuntimeNodePool nodePool,
            ScheduledExecutorService schedulerExecutor,
            int intervalSeconds
    ) {
        this.healthChecker = healthChecker;
        this.nodePool = nodePool;
        this.schedulerExecutor = schedulerExecutor;
        this.intervalSeconds = Math.max(1, intervalSeconds);
    }

    /**
     * 启动健康检查调度。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public void start() {
        if (started.compareAndSet(false, true)) {
            LOGGER.info("[OCR健康检查] 启动周期健康检查调度, intervalSeconds={}", intervalSeconds);
            schedulerExecutor.scheduleWithFixedDelay(this::runSafely, 0, intervalSeconds, TimeUnit.SECONDS);
        } else {
            LOGGER.info("[OCR健康检查] 周期健康检查调度已启动, intervalSeconds={}", intervalSeconds);
        }
    }

    /**
     * 安全执行健康检查并刷新运行时池。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private void runSafely() {
        try {
            healthChecker.checkOnce();
            nodePool.refresh();
        } catch (RuntimeException ex) {
            LOGGER.warn("[OCR健康检查] 周期健康检查失败, error={}", ex.getMessage(), ex);
        }
    }
}
