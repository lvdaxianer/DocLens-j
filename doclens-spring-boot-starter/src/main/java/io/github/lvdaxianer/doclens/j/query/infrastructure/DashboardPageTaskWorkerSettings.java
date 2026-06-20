package io.github.lvdaxianer.doclens.j.query.infrastructure;

import java.util.Map;

/**
 * 页任务 worker 生效配置。
 *
 * @param execution 页任务 worker 执行配置
 * @param recovery 页任务 worker 恢复配置
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
public record DashboardPageTaskWorkerSettings(
        Execution execution,
        Recovery recovery
) {

    /** 页任务批量大小指标键。 */
    private static final String BATCH_SIZE_KEY = "batch_size";
    /** 页任务锁秒数指标键。 */
    private static final String LOCK_SECONDS_KEY = "lock_seconds";
    /** 线程池大小指标键。 */
    private static final String POOL_SIZE_KEY = "pool_size";
    /** 队列容量指标键。 */
    private static final String QUEUE_CAPACITY_KEY = "queue_capacity";
    /** 恢复数量上限指标键。 */
    private static final String RECOVERY_LIMIT_KEY = "recovery_limit";
    /** 调度间隔毫秒指标键。 */
    private static final String INTERVAL_MILLIS_KEY = "interval_millis";

    /**
     * 转换为 Dashboard 指标键值。
     *
     * @return Dashboard 指标键值
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    Map<String, Object> toMetrics() {
        return Map.ofEntries(
                Map.entry(BATCH_SIZE_KEY, execution.batchSize()),
                Map.entry(LOCK_SECONDS_KEY, execution.lockSeconds()),
                Map.entry(POOL_SIZE_KEY, execution.poolSize()),
                Map.entry(QUEUE_CAPACITY_KEY, execution.queueCapacity()),
                Map.entry(RECOVERY_LIMIT_KEY, recovery.recoveryLimit()),
                Map.entry(INTERVAL_MILLIS_KEY, recovery.intervalMillis())
        );
    }

    /**
     * 页任务 worker 执行配置。
     *
     * @param batchSize 每轮抢占页任务数量
     * @param lockSeconds 页任务锁秒数
     * @param poolSize 页任务执行线程数
     * @param queueCapacity 页任务执行队列容量
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    public record Execution(int batchSize, int lockSeconds, int poolSize, int queueCapacity) {
    }

    /**
     * 页任务 worker 恢复配置。
     *
     * @param recoveryLimit 每轮恢复过期页任务数量
     * @param intervalMillis 页任务扫描间隔毫秒
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    public record Recovery(int recoveryLimit, int intervalMillis) {
    }
}
