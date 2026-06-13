package io.github.lvdaxianer.doclens.j.health.application;

import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthRepository;
import java.util.Objects;

/**
 * 模型心跳池与持久化仓储同步服务。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public class ModelHeartbeatSyncService {

    private final ModelHeartbeatPool pool;
    private final ModelHealthRepository repository;

    /**
     * 创建模型心跳同步服务。
     *
     * @param pool 模型心跳池
     * @param repository 模型健康仓储
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public ModelHeartbeatSyncService(ModelHeartbeatPool pool, ModelHealthRepository repository) {
        this.pool = Objects.requireNonNull(pool, "model heartbeat pool is required");
        this.repository = Objects.requireNonNull(repository, "model health repository is required");
    }

    /**
     * 将当前内存心跳池快照刷新到仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public void flushToRepository() {
        repository.upsertAll(pool.snapshot());
    }

    /**
     * 从仓储刷新模型健康快照到内存心跳池。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public void refreshFromRepository() {
        pool.mergeFromRepository(repository.listAll());
    }
}
