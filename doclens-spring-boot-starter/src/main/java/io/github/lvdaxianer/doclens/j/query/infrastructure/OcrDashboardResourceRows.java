package io.github.lvdaxianer.doclens.j.query.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrModelRegistry;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeMetrics;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrRuntimeNodePool;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * OCR Dashboard 资源行组装器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
final class OcrDashboardResourceRows {

    private final OcrModelRegistry modelRegistry;
    private final OcrRuntimeNodePool nodePool;

    /**
     * 创建 OCR Dashboard 资源行组装器。
     *
     * @param modelRegistry OCR 模型注册表
     * @param nodePool OCR 运行时节点池
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    OcrDashboardResourceRows(OcrModelRegistry modelRegistry, OcrRuntimeNodePool nodePool) {
        this.modelRegistry = modelRegistry;
        this.nodePool = nodePool;
    }

    /**
     * 组装 OCR 模型并发容量行。
     *
     * @param nodes OCR 节点集合
     * @return 模型容量行
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    List<Map<String, Object>> modelRows(List<OcrNode> nodes) {
        return nodes.stream().collect(Collectors.groupingBy(OcrNode::modelKey)).entrySet().stream()
                .map(entry -> modelRow(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * 组装 OCR 节点表展示指标。
     *
     * @param nodes OCR 节点集合
     * @param metricsByNodeId 节点指标映射
     * @return 节点展示指标
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    List<Map<String, Object>> nodeRows(List<OcrNode> nodes, Map<String, OcrNodeMetrics> metricsByNodeId) {
        return nodes.stream().map(node -> nodeRow(node, metricsByNodeId)).toList();
    }

    /**
     * 创建模型容量行。
     *
     * @param modelKey OCR 模型 key
     * @param nodes 模型节点
     * @return 模型容量行
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private Map<String, Object> modelRow(String modelKey, List<OcrNode> nodes) {
        return Map.ofEntries(
                Map.entry("model_key", modelKey),
                Map.entry("model_name", modelName(modelKey)),
                Map.entry("inflight_images", inflightImages(nodes)),
                Map.entry("max_concurrency", maxConcurrency(nodes))
        );
    }

    /**
     * 组装单个 OCR 节点展示指标。
     *
     * @param node OCR 节点
     * @param metricsByNodeId 节点指标映射
     * @return 节点展示指标
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private Map<String, Object> nodeRow(OcrNode node, Map<String, OcrNodeMetrics> metricsByNodeId) {
        OcrNodeMetrics metrics = metricsByNodeId.getOrDefault(node.id(),
                new OcrNodeMetrics(0, 0, 0L, 0L, 0L, 0L, 0L, Optional.empty(), Optional.empty()));
        return Map.ofEntries(
                Map.entry("node_id", node.id()),
                Map.entry("model_key", node.modelKey()),
                Map.entry("model_name", modelName(node.modelKey())),
                Map.entry("node_name", node.name()),
                Map.entry("status", node.status().name()),
                Map.entry("max_concurrency", node.maxConcurrency()),
                Map.entry("inflight_images", inflightImages(node.id())),
                Map.entry("last_health_at", node.lastHealthAt().map(OffsetDateTime::toString).orElse("")),
                Map.entry("last_error", node.lastError().orElse("")),
                Map.entry("processed_images_today", metrics.processedImagesToday()),
                Map.entry("success_images", metrics.successImages()),
                Map.entry("failed_images", metrics.failedImages()),
                Map.entry("avg_latency_ms", metrics.avgLatencyMs()),
                Map.entry("p95_latency_ms", metrics.p95LatencyMs())
        );
    }

    /**
     * 返回模型展示名称。
     *
     * @param modelKey OCR 模型 key
     * @return 模型展示名称
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private String modelName(String modelKey) {
        return modelRegistry.find(modelKey).map(model -> model.name()).orElse(modelKey);
    }

    /**
     * 统计节点总运行中图片数。
     *
     * @param nodes OCR 节点集合
     * @return 运行中图片数
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private int inflightImages(List<OcrNode> nodes) {
        return nodes.stream().mapToInt(node -> inflightImages(node.id())).sum();
    }

    /**
     * 读取节点运行中图片数。
     *
     * @param nodeId 节点 ID
     * @return 运行中图片数
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private int inflightImages(String nodeId) {
        return nodePool.find(nodeId).map(node -> node.toView().inflightImages()).orElse(0);
    }

    /**
     * 统计节点总最大并发。
     *
     * @param nodes OCR 节点集合
     * @return 最大并发容量
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private int maxConcurrency(List<OcrNode> nodes) {
        return nodes.stream().filter(node -> node.status() != OcrNodeStatus.DISABLED)
                .mapToInt(OcrNode::maxConcurrency).sum();
    }
}
