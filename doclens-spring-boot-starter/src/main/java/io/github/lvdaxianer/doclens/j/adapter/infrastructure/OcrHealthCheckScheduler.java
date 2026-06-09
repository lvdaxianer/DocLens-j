package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;
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
    private final IntSupplier intervalSecondsSupplier;
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
        this(healthChecker, nodePool, schedulerExecutor, () -> intervalSeconds);
    }

    /**
     * 创建读取运行时探测间隔的 OCR 健康检查周期调度器。
     *
     * @param healthChecker OCR 健康检查器
     * @param nodePool OCR 运行时节点池
     * @param schedulerExecutor 健康检查调度线程池
     * @param intervalSecondsSupplier 当前探测间隔提供器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrHealthCheckScheduler(
            OcrHealthChecker healthChecker,
            OcrRuntimeNodePool nodePool,
            ScheduledExecutorService schedulerExecutor,
            IntSupplier intervalSecondsSupplier
    ) {
        this.healthChecker = healthChecker;
        this.nodePool = nodePool;
        this.schedulerExecutor = schedulerExecutor;
        this.intervalSecondsSupplier = intervalSecondsSupplier;
    }

    /**
     * 启动健康检查调度。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public void start() {
        if (started.compareAndSet(false, true)) {
            int intervalSeconds = intervalSeconds();
            LOGGER.info("[OCR健康检查] 启动周期健康检查调度, intervalSeconds={}", intervalSeconds);
            scheduleNext(0);
        } else {
            LOGGER.info("[OCR健康检查] 周期健康检查调度已启动, intervalSeconds={}", intervalSeconds());
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
        } finally {
            if (started.get()) {
                scheduleNext(intervalSeconds());
            } else {
                // 调度尚未启动时无需继续排下一轮任务。
            }
        }
    }

    /**
     * 安排下一轮健康检查。
     *
     * @param delaySeconds 延迟秒数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void scheduleNext(int delaySeconds) {
        if (!schedulerExecutor.isShutdown()) {
            schedulerExecutor.schedule(this::runSafely, Math.max(0, delaySeconds),
                    java.util.concurrent.TimeUnit.SECONDS);
        } else {
            // 调度器关闭后不再排新任务，避免销毁阶段抛出拒绝执行异常。
        }
    }

    /**
     * 读取当前生效的探测间隔秒数。
     *
     * @return 探测间隔秒数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private int intervalSeconds() {
        return Math.max(1, intervalSecondsSupplier.getAsInt());
    }
}
