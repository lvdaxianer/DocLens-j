package io.github.lvdaxianer.doclens.j.health.application;

/**
 * 模型心跳 worker 设置。
 *
 * @param intervalSeconds 正常探测间隔秒数
 * @param retryIntervalSeconds 失败重试间隔秒数
 * @param maxAttempts 单轮最大尝试次数
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record ModelHeartbeatWorkerSettings(
        int intervalSeconds,
        int retryIntervalSeconds,
        int maxAttempts
) {

    private static final int DEFAULT_INTERVAL_SECONDS = 5;
    private static final int DEFAULT_RETRY_INTERVAL_SECONDS = 1;
    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    /**
     * 创建默认模型心跳 worker 设置。
     *
     * @return 默认模型心跳 worker 设置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static ModelHeartbeatWorkerSettings defaults() {
        return new ModelHeartbeatWorkerSettings(DEFAULT_INTERVAL_SECONDS, DEFAULT_RETRY_INTERVAL_SECONDS,
                DEFAULT_MAX_ATTEMPTS);
    }

    /**
     * 创建模型心跳 worker 设置。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public ModelHeartbeatWorkerSettings {
        intervalSeconds = positiveOrDefault(intervalSeconds, DEFAULT_INTERVAL_SECONDS);
        retryIntervalSeconds = positiveOrDefault(retryIntervalSeconds, DEFAULT_RETRY_INTERVAL_SECONDS);
        maxAttempts = positiveOrDefault(maxAttempts, DEFAULT_MAX_ATTEMPTS);
    }

    /**
     * 返回正数或默认值。
     *
     * @param value 配置值
     * @param defaultValue 默认值
     * @return 正数配置值
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static int positiveOrDefault(int value, int defaultValue) {
        if (value > 0) {
            return value;
        } else {
            return defaultValue;
        }
    }
}
