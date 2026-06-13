package io.github.lvdaxianer.doclens.j.health.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 模型健康状态领域规则测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class ModelHealthStateTest {

    /**
     * 成功心跳应将未知状态置为可用并清理失败字段。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void successMovesUnknownToUpAndClearsFailureFields() {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");
        ModelHealthState state = ModelHealthState.initial(target());

        ModelHealthState updated = state.markSuccess(now);

        assertThat(updated.status()).isEqualTo(ModelHealthStatus.UP);
        assertThat(updated.consecutiveFailures()).isZero();
        assertThat(updated.lastHeartbeatAt()).contains(now);
        assertThat(updated.lastSuccessAt()).contains(now);
        assertThat(updated.lastFailureAt()).isEmpty();
        assertThat(updated.lastError()).isEmpty();
    }

    /**
     * 第三次连续失败应将可用模型置为不可用并记录失败类型。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void thirdFailureMovesUpToDownAndRecordsFailureType() {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");
        ModelHealthState state = ModelHealthState.initial(target()).markSuccess(now.minusSeconds(5));

        ModelHealthState once = state.markFailure(ModelHealthFailureType.TIMEOUT, "timeout", now.minusSeconds(2), 3);
        ModelHealthState twice = once.markFailure(ModelHealthFailureType.TIMEOUT, "timeout", now.minusSeconds(1), 3);
        ModelHealthState third = twice.markFailure(ModelHealthFailureType.TIMEOUT, "timeout", now, 3);

        assertThat(third.status()).isEqualTo(ModelHealthStatus.DOWN);
        assertThat(third.consecutiveFailures()).isEqualTo(3L);
        assertThat(third.lastFailureType()).contains(ModelHealthFailureType.TIMEOUT);
        assertThat(third.lastError()).contains("timeout");
    }

    /**
     * 不可用模型成功探测后应先进入恢复中，再按恢复阈值回到可用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void downMovesToRecoveringBeforeReturningUp() {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");
        ModelHealthState down = ModelHealthState.initial(target())
                .markFailure(ModelHealthFailureType.CONNECTION_REFUSED, "refused", now.minusSeconds(3), 1);

        ModelHealthState recovering = down.markSuccess(now.minusSeconds(1));
        ModelHealthState up = recovering.markRecoverySuccess(now, 2);

        assertThat(recovering.status()).isEqualTo(ModelHealthStatus.RECOVERING);
        assertThat(up.status()).isEqualTo(ModelHealthStatus.UP);
    }

    /**
     * 恢复成功次数不足阈值时应保持恢复中。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void recoveringStaysRecoveringBeforeSuccessThreshold() {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");
        ModelHealthState down = ModelHealthState.initial(target())
                .markFailure(ModelHealthFailureType.CONNECTION_REFUSED, "refused", now.minusSeconds(3), 1);

        ModelHealthState recovering = down.markSuccess(now.minusSeconds(1));
        ModelHealthState stillRecovering = recovering.markRecoverySuccess(now, 3);

        assertThat(stillRecovering.status()).isEqualTo(ModelHealthStatus.RECOVERING);
        assertThat(stillRecovering.consecutiveSuccesses()).isEqualTo(2L);
    }

    /**
     * 过期的可用快照恢复时应降级为未知状态。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void staleUpSnapshotBecomesUnknownOnRestore() {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");
        ModelHealthState state = ModelHealthState.restore(snapshot(ModelHealthStatus.UP, now.minusSeconds(30)),
                now, 20);

        assertThat(state.status()).isEqualTo(ModelHealthStatus.UNKNOWN);
    }

    /**
     * 禁用状态应忽略成功和失败标记。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void disabledStateIgnoresSuccessAndFailureMarks() {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");
        ModelHealthState disabled = ModelHealthState.restore(snapshot(ModelHealthStatus.DISABLED, now),
                now, 20);

        ModelHealthState afterSuccess = disabled.markSuccess(now.plusSeconds(1));
        ModelHealthState afterFailure = disabled.markFailure(ModelHealthFailureType.TIMEOUT, "timeout",
                now.plusSeconds(2), 1);

        assertThat(afterSuccess).isEqualTo(disabled);
        assertThat(afterFailure).isEqualTo(disabled);
    }

    /**
     * 创建测试用模型目标。
     *
     * @return 模型目标
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthTargetId target() {
        return new ModelHealthTargetId(ModelHealthTargetType.OCR_NODE, "paddle_ocr", "paddle-1");
    }

    /**
     * 创建测试用健康快照。
     *
     * @param status 状态
     * @param lastHeartbeatAt 最近心跳时间
     * @return 健康快照
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthSnapshot snapshot(ModelHealthStatus status, OffsetDateTime lastHeartbeatAt) {
        return new ModelHealthSnapshot(target(), status, ModelHealthCounters.empty(),
                new ModelHealthTimeline(Optional.of(lastHeartbeatAt), Optional.empty(), Optional.empty(),
                        lastHeartbeatAt),
                ModelHealthFailure.empty());
    }
}
