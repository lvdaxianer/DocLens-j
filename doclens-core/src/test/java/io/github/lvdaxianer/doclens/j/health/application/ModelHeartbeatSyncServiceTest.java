package io.github.lvdaxianer.doclens.j.health.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthCounters;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthFailure;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthRepository;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthSnapshot;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthStatus;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTargetId;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTargetType;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTimeline;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 模型心跳池同步服务测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class ModelHeartbeatSyncServiceTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");
    private static final int FAILURE_THRESHOLD = 3;
    private static final int RECOVERY_SUCCESS_THRESHOLD = 2;
    private static final int STALE_AFTER_SECONDS = 20;
    private static final String MODEL_KEY = "paddle_ocr";
    private static final String TARGET_ID = "paddle-1";

    /**
     * 刷新到仓储时应批量写入当前心跳池快照。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void flushWritesCurrentPoolSnapshotToRepository() {
        InMemoryModelHeartbeatPool pool = new InMemoryModelHeartbeatPool(settings());
        InMemoryRepository repository = new InMemoryRepository();
        ModelHeartbeatSyncService service = new ModelHeartbeatSyncService(pool, repository);

        pool.register(List.of(target()));
        pool.markSuccess(target());
        service.flushToRepository();

        assertThat(repository.saved()).singleElement()
                .extracting(ModelHealthSnapshot::status)
                .isEqualTo(ModelHealthStatus.UP);
    }

    /**
     * 从仓储刷新时应合并快照到心跳池。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void refreshMergesRepositorySnapshotIntoPool() {
        InMemoryModelHeartbeatPool pool = new InMemoryModelHeartbeatPool(settings());
        InMemoryRepository repository = new InMemoryRepository();
        ModelHeartbeatSyncService service = new ModelHeartbeatSyncService(pool, repository);
        repository.replaceWith(List.of(snapshot(ModelHealthStatus.DOWN, now())));

        service.refreshFromRepository();

        assertThat(pool.find(target())).get()
                .extracting(ModelHealthSnapshot::status)
                .isEqualTo(ModelHealthStatus.DOWN);
    }

    /**
     * 创建测试用心跳池设置。
     *
     * @return 心跳池设置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHeartbeatPoolSettings settings() {
        return new ModelHeartbeatPoolSettings(this::now, FAILURE_THRESHOLD, RECOVERY_SUCCESS_THRESHOLD,
                STALE_AFTER_SECONDS);
    }

    /**
     * 创建测试用模型目标。
     *
     * @return 模型目标
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthTargetId target() {
        return new ModelHealthTargetId(ModelHealthTargetType.OCR_NODE, MODEL_KEY, TARGET_ID);
    }

    /**
     * 返回固定当前时间。
     *
     * @return 当前时间
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OffsetDateTime now() {
        return BASE_TIME;
    }

    /**
     * 创建测试用健康快照。
     *
     * @param status 健康状态
     * @param updatedAt 更新时间
     * @return 健康快照
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthSnapshot snapshot(ModelHealthStatus status, OffsetDateTime updatedAt) {
        return new ModelHealthSnapshot(target(), status, ModelHealthCounters.empty(),
                new ModelHealthTimeline(Optional.of(updatedAt), Optional.empty(), Optional.empty(), updatedAt),
                ModelHealthFailure.empty());
    }

    /**
     * 测试用内存仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static class InMemoryRepository implements ModelHealthRepository {

        private List<ModelHealthSnapshot> saved = new ArrayList<>(0);

        /**
         * 查询全部模型健康快照。
         *
         * @return 模型健康快照集合
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        @Override
        public List<ModelHealthSnapshot> listAll() {
            return saved;
        }

        /**
         * 批量写入或更新模型健康快照。
         *
         * @param snapshots 模型健康快照集合
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        @Override
        public void upsertAll(List<ModelHealthSnapshot> snapshots) {
            saved = List.copyOf(snapshots);
        }

        /**
         * 替换仓储快照。
         *
         * @param snapshots 模型健康快照集合
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        private void replaceWith(List<ModelHealthSnapshot> snapshots) {
            saved = List.copyOf(snapshots);
        }

        /**
         * 返回已保存快照。
         *
         * @return 已保存快照集合
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        private List<ModelHealthSnapshot> saved() {
            return saved;
        }
    }
}
