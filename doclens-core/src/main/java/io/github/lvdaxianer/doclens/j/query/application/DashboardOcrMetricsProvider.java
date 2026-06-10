package io.github.lvdaxianer.doclens.j.query.application;

import java.util.List;
import java.util.Map;

/**
 * Dashboard OCR 资源指标提供器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public interface DashboardOcrMetricsProvider {

    /**
     * 获取 OCR 资源和线程池指标。
     *
     * @return OCR 资源指标
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    Map<String, Object> ocrResources();

    /**
     * 获取批次级 OCR 调度命中节点。
     *
     * @param batchId 批次 ID
     * @return OCR 调度命中节点列表
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    List<Map<String, Object>> dispatchHitNodesByBatch(String batchId);

    /**
     * 获取批次内各文档最终成功分配到的 OCR 节点。
     *
     * @param batchId 批次 ID
     * @return 文档最终分配节点映射
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    Map<String, List<Map<String, Object>>> finalHitNodesByBatch(String batchId);
}
