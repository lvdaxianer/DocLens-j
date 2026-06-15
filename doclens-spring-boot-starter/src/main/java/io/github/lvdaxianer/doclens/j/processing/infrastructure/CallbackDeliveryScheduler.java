package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 回调投递周期调度器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
public class CallbackDeliveryScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallbackDeliveryScheduler.class);

    private final Dependencies dependencies;
    private final int intervalMillis;
    private final AtomicBoolean started = new AtomicBoolean();

    /**
     * 创建回调投递周期调度器。
     *
     * @param dependencies 调度依赖
     * @param intervalMillis 调度间隔毫秒
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public CallbackDeliveryScheduler(
            Dependencies dependencies,
            int intervalMillis
    ) {
        this.dependencies = Objects.requireNonNull(dependencies, "callback scheduler dependencies is required");
        this.intervalMillis = Math.max(1, intervalMillis);
    }

    /**
     * 启动回调投递调度。
     *
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public void start() {
        if (started.compareAndSet(false, true)) {
            LOGGER.info("[回调投递] 启动回调投递调度, intervalMillis={}", intervalMillis);
            scheduleNext(0);
        } else {
            // 已启动时不重复注册调度任务。
        }
    }

    /**
     * 安排下一轮回调扫描。
     *
     * @param delayMillis 延迟毫秒
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private void scheduleNext(int delayMillis) {
        if (!dependencies.schedulerExecutor().isShutdown()) {
            dependencies.schedulerExecutor().schedule(this::runSafely, Math.max(0, delayMillis), TimeUnit.MILLISECONDS);
        } else {
            // 调度器关闭后不再继续排队。
        }
    }

    /**
     * 安全执行回调扫描。
     *
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private void runSafely() {
        try {
            dependencies.callbackExecutor().execute(this::executeWorkerSafely);
        } catch (RuntimeException ex) {
            LOGGER.warn("[回调投递] 提交回调扫描失败, error={}", ex.getMessage(), ex);
            rescheduleIfRunning();
        }
    }

    /**
     * 安全执行回调 worker。
     *
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private void executeWorkerSafely() {
        try {
            dependencies.callbackRunner().getAsInt();
        } catch (RuntimeException ex) {
            LOGGER.warn("[回调投递] 回调扫描执行失败, error={}", ex.getMessage(), ex);
        } finally {
            rescheduleIfRunning();
        }
    }

    /**
     * 在调度器运行时继续排下一轮。
     *
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private void rescheduleIfRunning() {
        if (started.get()) {
            scheduleNext(intervalMillis);
        } else {
            // 调度尚未启动或已停止时不继续排新任务。
        }
    }

    /**
     * 回调调度器依赖集合。
     *
     * @param callbackRunner 回调执行器
     * @param schedulerExecutor 调度线程池
     * @param callbackExecutor 回调线程池
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public record Dependencies(
            IntSupplier callbackRunner,
            ScheduledExecutorService schedulerExecutor,
            ExecutorService callbackExecutor
    ) {

        /**
         * 创建回调调度器依赖集合。
         *
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        public Dependencies {
            callbackRunner = Objects.requireNonNull(callbackRunner, "callback runner is required");
            schedulerExecutor = Objects.requireNonNull(schedulerExecutor, "scheduler executor is required");
            callbackExecutor = Objects.requireNonNull(callbackExecutor, "callback executor is required");
        }
    }
}
