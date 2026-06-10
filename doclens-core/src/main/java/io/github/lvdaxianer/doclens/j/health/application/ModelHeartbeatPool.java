package io.github.lvdaxianer.doclens.j.health.application;

import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthFailureType;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthSnapshot;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTargetId;
import java.util.List;
import java.util.Optional;

/**
 * 模型运行时心跳池。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public interface ModelHeartbeatPool {

    /**
     * 注册模型健康目标。
     *
     * @param targets 健康目标集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    void register(List<ModelHealthTargetId> targets);

    /**
     * 查询模型健康快照。
     *
     * @param targetId 健康目标标识
     * @return 模型健康快照
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    Optional<ModelHealthSnapshot> find(ModelHealthTargetId targetId);

    /**
     * 导出全部模型健康快照。
     *
     * @return 模型健康快照集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    List<ModelHealthSnapshot> snapshot();

    /**
     * 标记模型心跳或请求成功。
     *
     * @param targetId 健康目标标识
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    void markSuccess(ModelHealthTargetId targetId);

    /**
     * 标记模型心跳失败。
     *
     * @param targetId 健康目标标识
     * @param failureType 失败类型
     * @param message 失败摘要
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    void markHeartbeatFailure(ModelHealthTargetId targetId, ModelHealthFailureType failureType, String message);

    /**
     * 标记模型请求失败。
     *
     * @param targetId 健康目标标识
     * @param failureType 失败类型
     * @param message 失败摘要
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    void markRequestFailure(ModelHealthTargetId targetId, ModelHealthFailureType failureType, String message);

    /**
     * 合并持久化仓储快照。
     *
     * @param snapshots 持久化快照集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    void mergeFromRepository(List<ModelHealthSnapshot> snapshots);
}
