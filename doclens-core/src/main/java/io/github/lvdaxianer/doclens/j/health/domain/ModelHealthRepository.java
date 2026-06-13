package io.github.lvdaxianer.doclens.j.health.domain;

import java.util.List;

/**
 * 模型健康持久化仓储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public interface ModelHealthRepository {

    /**
     * 查询全部模型健康快照。
     *
     * @return 模型健康快照集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    List<ModelHealthSnapshot> listAll();

    /**
     * 批量写入或更新模型健康快照。
     *
     * @param snapshots 模型健康快照集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    void upsertAll(List<ModelHealthSnapshot> snapshots);
}
