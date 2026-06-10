package io.github.lvdaxianer.doclens.j.query.application;

import java.util.List;
import java.util.Map;

/**
 * 空 Dashboard OCR 指标提供器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public class EmptyDashboardOcrMetricsProvider implements DashboardOcrMetricsProvider {

    /**
     * 获取空 OCR 资源指标。
     *
     * @return 空 OCR 资源指标
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Override
    public Map<String, Object> ocrResources() {
        return Map.ofEntries(
                Map.entry("healthy_node_count", 0L),
                Map.entry("down_node_count", 0L),
                Map.entry("recovering_node_count", 0L),
                Map.entry("global_inflight_images", 0L),
                Map.entry("busiest_node", Map.of()),
                Map.entry("thread_pools", Map.of())
        );
    }

    /**
     * 获取空批次调度命中节点。
     *
     * @param batchId 批次 ID
     * @return 空命中节点列表
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public List<Map<String, Object>> dispatchHitNodesByBatch(String batchId) {
        return List.of();
    }

    /**
     * 获取空批次文档最终分配节点映射。
     *
     * @param batchId 批次 ID
     * @return 空最终分配节点映射
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public Map<String, List<Map<String, Object>>> finalHitNodesByBatch(String batchId) {
        return Map.of();
    }
}
