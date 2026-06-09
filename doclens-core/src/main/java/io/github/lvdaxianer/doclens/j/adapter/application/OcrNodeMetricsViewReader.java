package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeMetrics;
import java.util.List;
import java.util.Map;

/**
 * OCR 节点指标读取端口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public interface OcrNodeMetricsViewReader {

    /**
     * 批量读取节点指标。
     *
     * @param nodeIds 节点 ID 集合
     * @return 节点指标映射
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    Map<String, OcrNodeMetrics> metricsByNodeIds(List<String> nodeIds);
}
