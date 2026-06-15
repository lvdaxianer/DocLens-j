package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJob;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * 回调投递重试策略。
 *
 * @param maxRetries 最大重试次数
 * @param retryBackoff 重试退避间隔
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
record CallbackRetryPolicy(int maxRetries, Duration retryBackoff) {

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int DEFAULT_RETRY_BACKOFF_SECONDS = 30;

    /**
     * 创建带安全默认值的重试策略。
     *
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    CallbackRetryPolicy {
        maxRetries = Math.max(0, maxRetries);
        retryBackoff = retryBackoff == null ? defaultBackoff() : retryBackoff;
    }

    /**
     * 创建默认重试策略。
     *
     * @return 默认重试策略
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    static CallbackRetryPolicy defaults() {
        return new CallbackRetryPolicy(DEFAULT_MAX_RETRIES, defaultBackoff());
    }

    /**
     * 创建失败尝试信息。
     *
     * @param job 回调任务
     * @param now 当前时间
     * @return 失败尝试信息
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    CallbackRetryAttempt failureAttempt(CallbackJob job, OffsetDateTime now) {
        int nextRetryCount = job.retryCount() + 1;
        return new CallbackRetryAttempt(nextRetryCount, retryTime(nextRetryCount, now));
    }

    /**
     * 计算默认退避间隔。
     *
     * @return 默认退避间隔
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    static Duration defaultBackoff() {
        return Duration.ofSeconds(DEFAULT_RETRY_BACKOFF_SECONDS);
    }

    /**
     * 计算下次重试时间。
     *
     * @param nextRetryCount 下次重试次数
     * @param now 当前时间
     * @return 下次重试时间或空值
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private OffsetDateTime retryTime(int nextRetryCount, OffsetDateTime now) {
        // 未超过重试上限时，安排下一次重试。
        if (nextRetryCount <= maxRetries) {
            return now.plus(retryBackoff);
        } else {
            // 已达到重试上限时，不再安排重试时间。
            return null;
        }
    }

    /**
     * 回调失败后的重试尝试信息。
     *
     * @param retryCount 重试次数
     * @param nextRetryAt 下次重试时间
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    record CallbackRetryAttempt(int retryCount, OffsetDateTime nextRetryAt) {
    }
}
