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
     * 获取批次实际命中的 OCR 节点。
     *
     * @param batchId 批次 ID
     * @return OCR 命中节点列表
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    List<Map<String, Object>> hitNodesByBatch(String batchId);
}
