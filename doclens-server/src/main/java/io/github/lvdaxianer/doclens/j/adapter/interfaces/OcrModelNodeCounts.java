package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeMetrics;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OCR 模型节点聚合统计。
 *
 * @param nodeCount 节点数量
 * @param healthyNodeCount 健康节点数量
 * @param enabledNodeCount 启用节点数量
 * @param inflightImages 当前解析中图片数
 * @param maxConcurrency 总最大并发容量
 * @param enabledMaxConcurrency 启用节点并发容量
 * @param globalMaxConcurrency 全局调度并发容量
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
record OcrModelNodeCounts(
        int nodeCount,
        int healthyNodeCount,
        int enabledNodeCount,
        int inflightImages,
        int maxConcurrency,
        int enabledMaxConcurrency,
        int globalMaxConcurrency
) {

    /**
     * 从节点集合创建统计信息。
     *
     * @param nodes OCR 节点集合
     * @return 节点统计信息
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    static OcrModelNodeCounts from(List<OcrNode> nodes) {
        return from(nodes, Map.of());
    }

    /**
     * 从节点集合和指标创建统计信息。
     *
     * @param nodes OCR 节点集合
     * @param metricsByNodeId 节点指标映射
     * @return 节点统计信息
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    static OcrModelNodeCounts from(List<OcrNode> nodes, Map<String, OcrNodeMetrics> metricsByNodeId) {
        return new OcrModelNodeCounts(nodes.size(), healthyNodeCount(nodes), enabledNodeCount(nodes),
                inflightImages(nodes, metricsByNodeId), maxConcurrency(nodes), enabledMaxConcurrency(nodes),
                globalMaxConcurrency(nodes));
    }

    /**
     * 统计健康节点数量。
     *
     * @param nodes OCR 节点集合
     * @return 健康节点数量
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private static int healthyNodeCount(List<OcrNode> nodes) {
        return (int) nodes.stream().filter(node -> node.status() == OcrNodeStatus.UP).count();
    }

    /**
     * 统计启用节点数量。
     *
     * @param nodes OCR 节点集合
     * @return 启用节点数量
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private static int enabledNodeCount(List<OcrNode> nodes) {
        return (int) nodes.stream().filter(OcrNode::enabled).count();
    }

    /**
     * 统计模型当前解析中图片数。
     *
     * @param nodes OCR 节点集合
     * @param metricsByNodeId 节点指标映射
     * @return 当前解析中图片数
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private static int inflightImages(List<OcrNode> nodes, Map<String, OcrNodeMetrics> metricsByNodeId) {
        return nodes.stream().mapToInt(node -> metricsByNodeId.getOrDefault(node.id(), emptyMetrics())
                .inflightImages()).sum();
    }

    /**
     * 统计模型总最大并发容量。
     *
     * @param nodes OCR 节点集合
     * @return 总最大并发容量
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private static int maxConcurrency(List<OcrNode> nodes) {
        return nodes.stream().mapToInt(OcrNode::maxConcurrency).sum();
    }

    /**
     * 统计模型启用节点并发容量。
     *
     * @param nodes OCR 节点集合
     * @return 启用节点并发容量
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private static int enabledMaxConcurrency(List<OcrNode> nodes) {
        return nodes.stream().filter(OcrNode::enabled).mapToInt(OcrNode::maxConcurrency).sum();
    }

    /**
     * 统计模型全局调度并发容量。
     *
     * @param nodes OCR 节点集合
     * @return 全局调度并发容量
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private static int globalMaxConcurrency(List<OcrNode> nodes) {
        return nodes.stream().filter(OcrNode::enabled).filter(OcrNode::participateGlobal)
                .mapToInt(OcrNode::maxConcurrency).sum();
    }

    /**
     * 创建空 OCR 节点指标。
     *
     * @return 空 OCR 节点指标
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private static OcrNodeMetrics emptyMetrics() {
        return new OcrNodeMetrics(0, 0, 0L, 0L, 0L, 0L, 0L, Optional.empty(), Optional.empty());
    }
}
