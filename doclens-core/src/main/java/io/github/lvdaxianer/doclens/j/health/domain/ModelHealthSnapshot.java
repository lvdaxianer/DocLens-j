package io.github.lvdaxianer.doclens.j.health.domain;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 模型健康状态快照。
 *
 * @param targetId 健康目标标识
 * @param status 健康状态
 * @param counters 健康计数
 * @param timeline 健康时间线
 * @param failure 失败摘要
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record ModelHealthSnapshot(
        ModelHealthTargetId targetId,
        ModelHealthStatus status,
        ModelHealthCounters counters,
        ModelHealthTimeline timeline,
        ModelHealthFailure failure
) {

    /**
     * 创建模型健康状态快照。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public ModelHealthSnapshot {
        if (targetId == null) {
            throw new IllegalArgumentException("model health target id is required");
        } else {
            // 目标标识已提供，可以继续规范化快照字段。
        }
        status = status == null ? ModelHealthStatus.UNKNOWN : status;
        counters = counters == null ? ModelHealthCounters.empty() : counters;
        timeline = timeline == null ? ModelHealthTimeline.initial() : timeline;
        failure = failure == null ? ModelHealthFailure.empty() : failure;
    }

    /**
     * 返回连续失败次数。
     *
     * @return 连续失败次数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public long consecutiveFailures() {
        return counters.consecutiveFailures();
    }

    /**
     * 返回连续成功次数。
     *
     * @return 连续成功次数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public long consecutiveSuccesses() {
        return counters.consecutiveSuccesses();
    }

    /**
     * 返回最近心跳时间。
     *
     * @return 最近心跳时间
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public Optional<OffsetDateTime> lastHeartbeatAt() {
        return timeline.lastHeartbeatAt();
    }

    /**
     * 返回最近成功时间。
     *
     * @return 最近成功时间
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public Optional<OffsetDateTime> lastSuccessAt() {
        return timeline.lastSuccessAt();
    }

    /**
     * 返回最近失败时间。
     *
     * @return 最近失败时间
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public Optional<OffsetDateTime> lastFailureAt() {
        return timeline.lastFailureAt();
    }

    /**
     * 返回最近错误摘要。
     *
     * @return 最近错误摘要
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public Optional<String> lastError() {
        return failure.lastError();
    }

    /**
     * 返回最近失败类型。
     *
     * @return 最近失败类型
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public Optional<ModelHealthFailureType> lastFailureType() {
        return failure.lastFailureType();
    }

    /**
     * 返回更新时间。
     *
     * @return 更新时间
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OffsetDateTime updatedAt() {
        return timeline.updatedAt();
    }
}
