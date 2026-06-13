package io.github.lvdaxianer.doclens.j.health.domain;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 模型健康运行状态。
 *
 * @param targetId 健康目标标识
 * @param status 健康状态
 * @param counters 健康计数
 * @param timeline 健康时间线
 * @param failure 失败摘要
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record ModelHealthState(
        ModelHealthTargetId targetId,
        ModelHealthStatus status,
        ModelHealthCounters counters,
        ModelHealthTimeline timeline,
        ModelHealthFailure failure
) {

    /**
     * 创建模型健康运行状态。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public ModelHealthState {
        if (targetId == null) {
            throw new IllegalArgumentException("model health target id is required");
        } else {
            // 目标标识已提供，可以继续规范化健康字段。
        }
        status = status == null ? ModelHealthStatus.UNKNOWN : status;
        counters = counters == null ? ModelHealthCounters.empty() : counters;
        timeline = timeline == null ? ModelHealthTimeline.initial() : timeline;
        failure = failure == null ? ModelHealthFailure.empty() : failure;
    }

    /**
     * 创建初始模型健康状态。
     *
     * @param targetId 健康目标标识
     * @return 初始健康状态
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static ModelHealthState initial(ModelHealthTargetId targetId) {
        return new ModelHealthState(targetId, ModelHealthStatus.UNKNOWN, ModelHealthCounters.empty(),
                ModelHealthTimeline.initial(), ModelHealthFailure.empty());
    }

    /**
     * 从持久化快照恢复健康状态。
     *
     * @param snapshot 持久化快照
     * @param now 当前时间
     * @param staleAfterSeconds 过期秒数
     * @return 恢复后的健康状态
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static ModelHealthState restore(ModelHealthSnapshot snapshot, OffsetDateTime now, int staleAfterSeconds) {
        ModelHealthStatus restoredStatus = restoredStatus(snapshot, now, staleAfterSeconds);
        return new ModelHealthState(snapshot.targetId(), restoredStatus, snapshot.counters(), snapshot.timeline(),
                snapshot.failure());
    }

    /**
     * 标记一次健康成功。
     *
     * @param now 当前时间
     * @return 更新后的健康状态
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public ModelHealthState markSuccess(OffsetDateTime now) {
        if (status == ModelHealthStatus.DISABLED) {
            return this;
        } else if (status == ModelHealthStatus.DOWN) {
            return successState(ModelHealthStatus.RECOVERING, 1L, now);
        } else {
            return successState(ModelHealthStatus.UP, consecutiveSuccesses() + 1L, now);
        }
    }

    /**
     * 标记恢复期成功。
     *
     * @param now 当前时间
     * @param recoverySuccessThreshold 恢复成功阈值
     * @return 更新后的健康状态
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public ModelHealthState markRecoverySuccess(OffsetDateTime now, int recoverySuccessThreshold) {
        long successes = consecutiveSuccesses() + 1L;
        if (status == ModelHealthStatus.RECOVERING && successes >= Math.max(1, recoverySuccessThreshold)) {
            return successState(ModelHealthStatus.UP, 0L, now);
        } else if (status == ModelHealthStatus.RECOVERING) {
            return successState(ModelHealthStatus.RECOVERING, successes, now);
        } else {
            return markSuccess(now);
        }
    }

    /**
     * 标记一次健康失败。
     *
     * @param failureType 失败类型
     * @param error 错误摘要
     * @param now 当前时间
     * @param failureThreshold 失败阈值
     * @return 更新后的健康状态
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public ModelHealthState markFailure(
            ModelHealthFailureType failureType,
            String error,
            OffsetDateTime now,
            int failureThreshold
    ) {
        if (status == ModelHealthStatus.DISABLED) {
            return this;
        } else {
            return failedState(failureType, error, now, failureThreshold);
        }
    }

    /**
     * 转换为健康快照。
     *
     * @return 健康快照
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public ModelHealthSnapshot snapshot() {
        return new ModelHealthSnapshot(targetId, status, counters, timeline, failure);
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

    /**
     * 生成成功后的健康状态。
     *
     * @param nextStatus 新状态
     * @param successes 连续成功次数
     * @param now 当前时间
     * @return 成功后的健康状态
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthState successState(ModelHealthStatus nextStatus, long successes, OffsetDateTime now) {
        return new ModelHealthState(targetId, nextStatus, ModelHealthCounters.successes(successes),
                timeline.markSuccess(now), ModelHealthFailure.empty());
    }

    /**
     * 生成失败后的健康状态。
     *
     * @param failureType 失败类型
     * @param error 错误摘要
     * @param now 当前时间
     * @param failureThreshold 失败阈值
     * @return 失败后的健康状态
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthState failedState(
            ModelHealthFailureType failureType,
            String error,
            OffsetDateTime now,
            int failureThreshold
    ) {
        long failures = consecutiveFailures() + 1L;
        ModelHealthStatus nextStatus = failureStatus(failures, failureThreshold);
        return new ModelHealthState(targetId, nextStatus, ModelHealthCounters.failures(failures),
                timeline.markFailure(now), ModelHealthFailure.of(failureType, error));
    }

    /**
     * 根据失败次数计算失败状态。
     *
     * @param failures 失败次数
     * @param failureThreshold 失败阈值
     * @return 健康状态
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthStatus failureStatus(long failures, int failureThreshold) {
        if (failures >= Math.max(1, failureThreshold)) {
            return ModelHealthStatus.DOWN;
        } else {
            return status;
        }
    }

    /**
     * 根据快照新鲜度计算恢复状态。
     *
     * @param snapshot 持久化快照
     * @param now 当前时间
     * @param staleAfterSeconds 过期秒数
     * @return 恢复状态
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static ModelHealthStatus restoredStatus(
            ModelHealthSnapshot snapshot,
            OffsetDateTime now,
            int staleAfterSeconds
    ) {
        if (snapshot.status() == ModelHealthStatus.UP && isStale(snapshot, now, staleAfterSeconds)) {
            return ModelHealthStatus.UNKNOWN;
        } else {
            return snapshot.status();
        }
    }

    /**
     * 判断快照是否过期。
     *
     * @param snapshot 持久化快照
     * @param now 当前时间
     * @param staleAfterSeconds 过期秒数
     * @return 是否过期
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static boolean isStale(ModelHealthSnapshot snapshot, OffsetDateTime now, int staleAfterSeconds) {
        return snapshot.lastHeartbeatAt()
                .map(heartbeatAt -> heartbeatAt.plusSeconds(Math.max(1, staleAfterSeconds)).isBefore(now))
                .orElse(true);
    }
}
