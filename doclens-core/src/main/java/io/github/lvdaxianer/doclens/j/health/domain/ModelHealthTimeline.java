package io.github.lvdaxianer.doclens.j.health.domain;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 模型健康时间线。
 *
 * @param lastHeartbeatAt 最近心跳时间
 * @param lastSuccessAt 最近成功时间
 * @param lastFailureAt 最近失败时间
 * @param updatedAt 更新时间
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record ModelHealthTimeline(
        Optional<OffsetDateTime> lastHeartbeatAt,
        Optional<OffsetDateTime> lastSuccessAt,
        Optional<OffsetDateTime> lastFailureAt,
        OffsetDateTime updatedAt
) {

    /**
     * 创建模型健康时间线。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public ModelHealthTimeline {
        lastHeartbeatAt = optional(lastHeartbeatAt);
        lastSuccessAt = optional(lastSuccessAt);
        lastFailureAt = optional(lastFailureAt);
        updatedAt = updatedAt == null ? OffsetDateTime.now() : updatedAt;
    }

    /**
     * 创建初始时间线。
     *
     * @return 初始时间线
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static ModelHealthTimeline initial() {
        return new ModelHealthTimeline(Optional.empty(), Optional.empty(), Optional.empty(), OffsetDateTime.now());
    }

    /**
     * 创建成功后的时间线。
     *
     * @param now 当前时间
     * @return 成功时间线
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public ModelHealthTimeline markSuccess(OffsetDateTime now) {
        return new ModelHealthTimeline(Optional.of(now), Optional.of(now), Optional.empty(), now);
    }

    /**
     * 创建失败后的时间线。
     *
     * @param now 当前时间
     * @return 失败时间线
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public ModelHealthTimeline markFailure(OffsetDateTime now) {
        return new ModelHealthTimeline(Optional.of(now), lastSuccessAt, Optional.of(now), now);
    }

    /**
     * 规范化可选值。
     *
     * @param value 可选值
     * @return 非空可选值
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static Optional<OffsetDateTime> optional(Optional<OffsetDateTime> value) {
        if (value == null) {
            return Optional.empty();
        } else {
            return value;
        }
    }
}
