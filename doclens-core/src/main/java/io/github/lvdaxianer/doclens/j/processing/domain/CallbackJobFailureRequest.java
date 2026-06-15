package io.github.lvdaxianer.doclens.j.processing.domain;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * 回调任务失败更新请求。
 *
 * @param callbackJobId 回调任务 ID
 * @param failureReason 失败原因
 * @param failureDetail 失败详情
 * @param retryCount 重试次数
 * @param nextRetryAt 下次重试时间
 * @param now 更新时间
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
public record CallbackJobFailureRequest(
        String callbackJobId,
        CallbackFailureReason failureReason,
        String failureDetail,
        int retryCount,
        OffsetDateTime nextRetryAt,
        OffsetDateTime now
) {
    /**
     * 创建带安全默认值的失败更新请求。
     *
     * @param callbackJobId 回调任务 ID
     * @param failureReason 失败原因
     * @param failureDetail 失败详情
     * @param retryCount 重试次数
     * @param nextRetryAt 下次重试时间
     * @param now 更新时间
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public CallbackJobFailureRequest {
        failureReason = Objects.requireNonNull(failureReason, "callback failure reason is required");
        failureDetail = failureDetail == null ? "" : failureDetail;
    }
}
