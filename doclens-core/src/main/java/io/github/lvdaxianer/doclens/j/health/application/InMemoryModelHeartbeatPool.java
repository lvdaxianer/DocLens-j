package io.github.lvdaxianer.doclens.j.health.application;

import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthFailureType;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthSnapshot;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthState;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTargetId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 线程安全的内存模型心跳池。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public class InMemoryModelHeartbeatPool implements ModelHeartbeatPool {

    private final Map<ModelHealthTargetId, ModelHealthState> states = new ConcurrentHashMap<>();
    private final ModelHeartbeatPoolSettings settings;

    /**
     * 创建内存模型心跳池。
     *
     * @param settings 心跳池设置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public InMemoryModelHeartbeatPool(ModelHeartbeatPoolSettings settings) {
        this.settings = settings == null ? new ModelHeartbeatPoolSettings(OffsetDateTime::now, 3, 2, 20) : settings;
    }

    @Override
    public void register(List<ModelHealthTargetId> targets) {
        safeTargets(targets).forEach(this::registerTarget);
    }

    @Override
    public Optional<ModelHealthSnapshot> find(ModelHealthTargetId targetId) {
        return Optional.ofNullable(states.get(targetId)).map(ModelHealthState::snapshot);
    }

    @Override
    public List<ModelHealthSnapshot> snapshot() {
        return states.values().stream().map(ModelHealthState::snapshot).toList();
    }

    @Override
    public void markSuccess(ModelHealthTargetId targetId) {
        states.compute(targetId, (key, state) -> currentState(key, state).markSuccess(now()));
    }

    @Override
    public void markHeartbeatFailure(ModelHealthTargetId targetId, ModelHealthFailureType failureType, String message) {
        markFailure(targetId, failureType, message);
    }

    @Override
    public void markRequestFailure(ModelHealthTargetId targetId, ModelHealthFailureType failureType, String message) {
        markFailure(targetId, failureType, message);
    }

    @Override
    public void mergeFromRepository(List<ModelHealthSnapshot> snapshots) {
        safeSnapshots(snapshots).forEach(this::mergeSnapshot);
    }

    /**
     * 注册单个健康目标。
     *
     * @param targetId 健康目标标识
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void registerTarget(ModelHealthTargetId targetId) {
        states.putIfAbsent(targetId, ModelHealthState.initial(targetId));
    }

    /**
     * 标记模型失败。
     *
     * @param targetId 健康目标标识
     * @param failureType 失败类型
     * @param message 失败摘要
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void markFailure(ModelHealthTargetId targetId, ModelHealthFailureType failureType, String message) {
        states.compute(targetId, (key, state) -> currentState(key, state)
                .markFailure(failureType, message, now(), settings.failureThreshold()));
    }

    /**
     * 合并单个持久化快照。
     *
     * @param snapshot 持久化快照
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void mergeSnapshot(ModelHealthSnapshot snapshot) {
        states.compute(snapshot.targetId(), (key, current) -> newerState(snapshot, current));
    }

    /**
     * 根据快照新鲜度生成最新状态。
     *
     * @param snapshot 持久化快照
     * @param current 当前内存状态
     * @return 最新状态
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthState newerState(ModelHealthSnapshot snapshot, ModelHealthState current) {
        ModelHealthState restored = ModelHealthState.restore(snapshot, now(), settings.staleAfterSeconds());
        if (current == null || restored.updatedAt().isAfter(current.updatedAt())) {
            return restored;
        } else {
            return current;
        }
    }

    /**
     * 返回当前时间。
     *
     * @return 当前时间
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OffsetDateTime now() {
        return settings.nowSupplier().get();
    }

    /**
     * 返回当前状态或初始状态。
     *
     * @param targetId 健康目标标识
     * @param state 当前状态
     * @return 当前状态或初始状态
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthState currentState(ModelHealthTargetId targetId, ModelHealthState state) {
        if (state == null) {
            return ModelHealthState.initial(targetId);
        } else {
            return state;
        }
    }

    /**
     * 规范化目标集合。
     *
     * @param targets 健康目标集合
     * @return 非空健康目标集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private List<ModelHealthTargetId> safeTargets(List<ModelHealthTargetId> targets) {
        if (targets == null) {
            return List.of();
        } else {
            return targets;
        }
    }

    /**
     * 规范化快照集合。
     *
     * @param snapshots 健康快照集合
     * @return 非空健康快照集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private List<ModelHealthSnapshot> safeSnapshots(List<ModelHealthSnapshot> snapshots) {
        if (snapshots == null) {
            return List.of();
        } else {
            return snapshots;
        }
    }
}
