package io.github.lvdaxianer.doclens.j.health.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthCounters;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthFailure;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthFailureType;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthSnapshot;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthStatus;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTargetId;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTargetType;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTimeline;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 内存模型心跳池测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class InMemoryModelHeartbeatPoolTest {

    /**
     * 成功心跳应立即更新内存池状态。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void markSuccessUpdatesPoolImmediately() {
        InMemoryModelHeartbeatPool pool = new InMemoryModelHeartbeatPool(settings());
        ModelHealthTargetId target = target();

        pool.register(List.of(target));
        pool.markSuccess(target);

        assertThat(pool.find(target)).get()
                .extracting(ModelHealthSnapshot::status)
                .isEqualTo(ModelHealthStatus.UP);
    }

    /**
     * 请求连续三次失败后应将目标置为不可用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void thirdRequestFailureMarksTargetDown() {
        InMemoryModelHeartbeatPool pool = new InMemoryModelHeartbeatPool(settings());
        ModelHealthTargetId target = target();

        pool.register(List.of(target));
        pool.markRequestFailure(target, ModelHealthFailureType.REQUEST_FAILED, "ocr failed");
        pool.markRequestFailure(target, ModelHealthFailureType.REQUEST_FAILED, "ocr failed");
        pool.markRequestFailure(target, ModelHealthFailureType.REQUEST_FAILED, "ocr failed");

        assertThat(pool.find(target())).get()
                .extracting(ModelHealthSnapshot::status)
                .isEqualTo(ModelHealthStatus.DOWN);
    }

    /**
     * 较旧的仓储快照不应覆盖较新的运行时状态。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void olderRepositorySnapshotDoesNotOverrideFreshRuntimeState() {
        InMemoryModelHeartbeatPool pool = new InMemoryModelHeartbeatPool(settings());
        ModelHealthTargetId target = target();

        pool.register(List.of(target));
        pool.markSuccess(target);
        pool.mergeFromRepository(List.of(snapshot(target, ModelHealthStatus.DOWN, now().minusSeconds(5))));

        assertThat(pool.find(target)).get()
                .extracting(ModelHealthSnapshot::status)
                .isEqualTo(ModelHealthStatus.UP);
    }

    /**
     * 创建测试用心跳池设置。
     *
     * @return 心跳池设置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHeartbeatPoolSettings settings() {
        return new ModelHeartbeatPoolSettings(this::now, 3, 2, 20);
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
     * 返回固定当前时间。
     *
     * @return 当前时间
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OffsetDateTime now() {
        return OffsetDateTime.parse("2026-06-10T10:00:00+08:00");
    }

    /**
     * 创建测试用健康快照。
     *
     * @param target 目标标识
     * @param status 健康状态
     * @param updatedAt 更新时间
     * @return 健康快照
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthSnapshot snapshot(
            ModelHealthTargetId target,
            ModelHealthStatus status,
            OffsetDateTime updatedAt
    ) {
        return new ModelHealthSnapshot(target, status, ModelHealthCounters.empty(),
                new ModelHealthTimeline(Optional.of(updatedAt), Optional.empty(), Optional.empty(), updatedAt),
                ModelHealthFailure.empty());
    }
}
