package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import java.util.List;
import java.util.Optional;

/**
 * OCR 节点选择器端口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public interface OcrNodeSelector {

    /**
     * 根据路由策略从候选节点中选择一个节点。
     *
     * @param policy OCR 路由策略
     * @param nodes 候选节点
     * @return 选中的节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    Optional<OcrRuntimeNodeView> select(OcrRoutePolicy policy, List<OcrRuntimeNodeView> nodes);
}
