package io.github.lvdaxianer.doclens.j.adapter.application;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * OCR 路由累计状态。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class OcrRouteAccumulator {

    private final OffsetDateTime startedAt;
    private int retryCount;
    private Optional<RuntimeException> lastFailure = Optional.empty();

    /**
     * 创建路由累计状态。
     *
     * @param startedAt 开始时间
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    OcrRouteAccumulator(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * 记录一次失败。
     *
     * @param failure 失败异常
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    void recordFailure(RuntimeException failure) {
        retryCount++;
        lastFailure = Optional.of(failure);
    }

    /**
     * 返回开始时间。
     *
     * @return 开始时间
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    OffsetDateTime startedAt() {
        return startedAt;
    }

    /**
     * 返回累计失败重试次数。
     *
     * @return 累计失败重试次数
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    int retryCount() {
        return retryCount;
    }

    /**
     * 返回最近一次失败。
     *
     * @return 最近一次失败
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    Optional<RuntimeException> lastFailure() {
        return lastFailure;
    }

    /**
     * 返回从开始到当前的耗时。
     *
     * @return 耗时毫秒
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    long elapsedMs() {
        return Duration.between(startedAt, OffsetDateTime.now()).toMillis();
    }
}
