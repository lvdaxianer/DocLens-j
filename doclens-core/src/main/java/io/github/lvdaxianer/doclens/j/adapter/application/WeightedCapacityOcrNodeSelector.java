package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutingMode;
import java.util.List;
import java.util.Optional;

/**
 * 基于空闲容量和权重的 OCR 节点选择器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public final class WeightedCapacityOcrNodeSelector implements OcrNodeSelector {

    private final double idleFactor;
    private final double weightFactor;
    private final double topBucketThreshold;
    private final SmoothWeightedRoundRobinState roundRobinState = new SmoothWeightedRoundRobinState();

    /**
     * 创建加权容量 OCR 节点选择器。
     *
     * @param idleFactor 空闲容量权重
     * @param weightFactor 节点权重权重
     * @param topBucketThreshold Top Bucket 阈值
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public WeightedCapacityOcrNodeSelector(double idleFactor, double weightFactor, double topBucketThreshold) {
        this.idleFactor = idleFactor;
        this.weightFactor = weightFactor;
        this.topBucketThreshold = topBucketThreshold;
    }

    /**
     * 选择一个最适合的 OCR 节点。
     *
     * @param policy OCR 路由策略
     * @param nodes 候选节点
     * @return 选中的节点
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public Optional<OcrRuntimeNodeView> select(OcrRoutePolicy policy, List<OcrRuntimeNodeView> nodes) {
        List<OcrRuntimeNodeView> candidates = nodes.stream()
                .filter(node -> node.enabled())
                .filter(node -> node.status() == OcrNodeStatus.UP)
                .filter(node -> node.availableSlots() > 0)
                .filter(node -> matchesPolicy(policy, node))
                .toList();
        if (candidates.isEmpty()) {
            return Optional.empty();
        } else {
            return selectTopBucket(candidates);
        }
    }

    /**
     * 计算 Top Bucket 并执行平滑加权轮转。
     *
     * @param candidates 可用候选节点
     * @return 选中的节点
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private Optional<OcrRuntimeNodeView> selectTopBucket(List<OcrRuntimeNodeView> candidates) {
        long totalWeight = candidates.stream().mapToLong(OcrRuntimeNodeView::weight).sum();
        List<OcrNodeScore> scores = candidates.stream().map(node -> score(node, totalWeight)).toList();
        double topScore = scores.stream().mapToDouble(OcrNodeScore::score).max().orElse(0D);
        List<OcrRuntimeNodeView> topBucket = scores.stream()
                .filter(score -> topScore - score.score() <= topBucketThreshold)
                .map(OcrNodeScore::node)
                .toList();
        return roundRobinState.next(topBucket);
    }

    /**
     * 计算单个节点的评分。
     *
     * @param node 节点
     * @param totalWeight 候选总权重
     * @return 节点评分
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrNodeScore score(OcrRuntimeNodeView node, long totalWeight) {
        double idleRatio = node.maxConcurrency() <= 0 ? 0D : ((double) node.availableSlots()) / node.maxConcurrency();
        double weightRatio = totalWeight <= 0 ? 0D : ((double) node.weight()) / totalWeight;
        double score = idleRatio * idleFactor + weightRatio * weightFactor;
        return new OcrNodeScore(node, idleRatio, weightRatio, score);
    }

    /**
     * 判断节点是否匹配当前路由策略。
     *
     * @param policy OCR 路由策略
     * @param node 运行时节点
     * @return 是否匹配
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private boolean matchesPolicy(OcrRoutePolicy policy, OcrRuntimeNodeView node) {
        if (policy.routingMode() == OcrRoutingMode.MODEL_LOAD_BALANCE) {
            return policy.modelKey().filter(node.modelKey()::equals).isPresent();
        } else {
            return matchesNonModelPolicy(policy, node);
        }
    }

    /**
     * 判断节点是否匹配非模型负载均衡策略。
     *
     * @param policy OCR 路由策略
     * @param node 运行时节点
     * @return 是否匹配
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private boolean matchesNonModelPolicy(OcrRoutePolicy policy, OcrRuntimeNodeView node) {
        if (policy.routingMode() == OcrRoutingMode.SPECIFIC_NODE) {
            return policy.nodeId().filter(node.nodeId()::equals).isPresent();
        } else {
            return node.participateGlobal();
        }
    }
}
