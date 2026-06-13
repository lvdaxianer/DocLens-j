package io.github.lvdaxianer.doclens.j.health.application;

import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthFailureType;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTargetId;
import java.time.Duration;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 单目标串行模型心跳 worker。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public class ModelHeartbeatWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelHeartbeatWorker.class);
    private static final String INTERRUPTED_MESSAGE = "heartbeat retry interrupted";
    private static final int STOP_ATTEMPT = Integer.MAX_VALUE;

    private final ModelHealthTargetId targetId;
    private final ModelHeartbeatProbe probe;
    private final ModelHeartbeatPool pool;
    private final Sleeper sleeper;
    private final ModelHeartbeatWorkerSettings settings;

    /**
     * 创建单目标模型心跳 worker。
     *
     * @param dependencies worker 依赖
     * @param settings worker 设置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public ModelHeartbeatWorker(Dependencies dependencies, ModelHeartbeatWorkerSettings settings) {
        Dependencies safeDependencies = Objects.requireNonNull(dependencies, "model heartbeat dependencies required");
        this.targetId = safeDependencies.targetId();
        this.probe = safeDependencies.probe();
        this.pool = safeDependencies.pool();
        this.sleeper = safeDependencies.sleeper();
        this.settings = settings == null ? ModelHeartbeatWorkerSettings.defaults() : settings;
    }

    /**
     * 执行一轮串行心跳探测。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public void runOnce() {
        int attempt = 1;
        while (attempt <= settings.maxAttempts()) {
            ModelHeartbeatProbeResult result = probe.probe();
            // 探测成功时立即更新心跳池，并结束当前串行探测轮次。
            if (result.isSuccessful()) {
                pool.markSuccess(targetId);
                return;
            } else {
                // 探测失败时先记录失败，再按配置决定是否等待下一次重试。
                pool.markHeartbeatFailure(targetId, result.failureType(), result.message());
                attempt = retryNext(attempt).nextAttempt();
            }
        }
    }

    /**
     * 失败后等待下一次重试。
     *
     * @param attempt 当前尝试次数
     * @return 重试决策
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private RetryDecision retryNext(int attempt) {
        // 当前尝试次数未达到上限时，等待失败重试间隔后继续。
        if (attempt < settings.maxAttempts()) {
            return sleepBeforeRetry(attempt);
        } else {
            // 已达到最大尝试次数，本轮探测结束。
            return RetryDecision.stop();
        }
    }

    /**
     * 在失败重试前休眠。
     *
     * @param attempt 当前尝试次数
     * @return 重试决策
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private RetryDecision sleepBeforeRetry(int attempt) {
        try {
            sleeper.sleep(Duration.ofSeconds(settings.retryIntervalSeconds()));
            return RetryDecision.next(attempt + 1);
        } catch (InterruptedException exception) {
            // 重试等待被中断时恢复中断标记，并停止当前探测轮次。
            Thread.currentThread().interrupt();
            LOGGER.warn("[模型心跳] 心跳失败重试被中断, targetType={}, modelKey={}, targetId={}",
                    targetId.targetType(), targetId.modelKey(), targetId.targetId(), exception);
            pool.markHeartbeatFailure(targetId, ModelHealthFailureType.UNKNOWN, INTERRUPTED_MESSAGE);
            return RetryDecision.stop();
        }
    }

    /**
     * 重试决策。
     *
     * @param nextAttempt 下一次尝试次数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private record RetryDecision(int nextAttempt) {

        /**
         * 创建下一次尝试决策。
         *
         * @param nextAttempt 下一次尝试次数
         * @return 重试决策
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        private static RetryDecision next(int nextAttempt) {
            return new RetryDecision(nextAttempt);
        }

        /**
         * 创建停止决策。
         *
         * @return 重试决策
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        private static RetryDecision stop() {
            return new RetryDecision(STOP_ATTEMPT);
        }
    }

    /**
     * 模型心跳 worker 依赖。
     *
     * @param targetId 健康目标标识
     * @param probe 心跳探针
     * @param pool 心跳池
     * @param sleeper 休眠器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public record Dependencies(
            ModelHealthTargetId targetId,
            ModelHeartbeatProbe probe,
            ModelHeartbeatPool pool,
            Sleeper sleeper
    ) {

        /**
         * 创建模型心跳 worker 依赖。
         *
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        public Dependencies {
            targetId = Objects.requireNonNull(targetId, "model health target id is required");
            probe = Objects.requireNonNull(probe, "model heartbeat probe is required");
            pool = Objects.requireNonNull(pool, "model heartbeat pool is required");
            sleeper = sleeper == null ? duration -> Thread.sleep(duration.toMillis()) : sleeper;
        }
    }

    /**
     * 休眠器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @FunctionalInterface
    public interface Sleeper {

        /**
         * 休眠指定时长。
         *
         * @param duration 休眠时长
         * @throws InterruptedException 线程中断时抛出
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        void sleep(Duration duration) throws InterruptedException;
    }
}
