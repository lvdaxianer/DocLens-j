package io.github.lvdaxianer.doclens.j.processing.domain;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * 回调任务聚合。
 *
 * @param callbackJobId 回调任务 ID
 * @param eventId 事件 ID
 * @param batchId 批次 ID
 * @param documentId 可选文档 ID
 * @param callbackUrl 回调地址
 * @param status 回调状态
 * @param payload 回调载荷
 * @param retryCount 重试次数
 * @param nextRetryAt 下次重试时间
 * @param failureReason 失败原因
 * @param failureDetail 失败详情
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
public record CallbackJob(
        String callbackJobId,
        String eventId,
        String batchId,
        Optional<String> documentId,
        String callbackUrl,
        CallbackJobStatus status,
        Map<String, Object> payload,
        int retryCount,
        Optional<OffsetDateTime> nextRetryAt,
        Optional<CallbackFailureReason> failureReason,
        Optional<String> failureDetail,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /**
     * 创建带安全默认值的回调任务。
     *
     * @param request 回调任务创建请求
     * @return 回调任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public static CallbackJob create(CallbackJobCreateRequest request) {
        return new CallbackJob(request.callbackJobId(), request.eventId(), request.batchId(),
                Optional.ofNullable(request.documentId()), request.callbackUrl(), CallbackJobStatus.PENDING,
                request.payload(), 0, Optional.empty(), Optional.empty(), Optional.empty(), request.now(),
                request.now());
    }

    /**
     * 标记回调任务进入重试状态。
     *
     * @param request 失败更新请求
     * @return 重试中的回调任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public CallbackJob markRetrying(CallbackJobFailureRequest request) {
        return withFailureState(CallbackJobStatus.RETRYING, request);
    }

    /**
     * 标记回调任务终态失败。
     *
     * @param request 失败更新请求
     * @return 终态失败的回调任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public CallbackJob markFailed(CallbackJobFailureRequest request) {
        return withFailureState(CallbackJobStatus.FAILED, request);
    }

    /**
     * 设置回调任务成功状态。
     *
     * @param now 更新时间
     * @return 成功的回调任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public CallbackJob markSucceeded(OffsetDateTime now) {
        return new CallbackJob(callbackJobId, eventId, batchId, documentId, callbackUrl,
                CallbackJobStatus.SUCCESS, payload, retryCount, Optional.empty(), Optional.empty(),
                Optional.empty(), createdAt, now);
    }

    /**
     * 创建失败状态的回调任务。
     *
     * @param status 目标状态
     * @param request 失败更新请求
     * @return 更新后的回调任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private CallbackJob withFailureState(CallbackJobStatus status, CallbackJobFailureRequest request) {
        return new CallbackJob(callbackJobId, eventId, batchId, documentId, callbackUrl, status, payload,
                request.retryCount(), Optional.ofNullable(request.nextRetryAt()), Optional.of(request.failureReason()),
                Optional.of(request.failureDetail()), createdAt, request.now());
    }
}
