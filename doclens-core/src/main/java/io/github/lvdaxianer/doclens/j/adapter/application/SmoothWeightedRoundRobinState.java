package io.github.lvdaxianer.doclens.j.adapter.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 平滑加权轮转状态。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public final class SmoothWeightedRoundRobinState {

    private final Map<String, Integer> currentWeights = new HashMap<>(8);

    /**
     * 从候选节点中选择下一个节点。
     *
     * @param nodes 候选节点
     * @return 选中的节点
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public Optional<OcrRuntimeNodeView> next(List<OcrRuntimeNodeView> nodes) {
        if (nodes.isEmpty()) {
            return Optional.empty();
        } else {
            cleanup(nodes);
            return Optional.of(select(nodes));
        }
    }

    /**
     * 清理已不在候选集中的状态。
     *
     * @param nodes 候选节点
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void cleanup(List<OcrRuntimeNodeView> nodes) {
        currentWeights.keySet().removeIf(nodeId -> nodes.stream().noneMatch(node -> node.nodeId().equals(nodeId)));
    }

    /**
     * 执行一次平滑加权轮转选择。
     *
     * @param nodes 候选节点
     * @return 选中的节点
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrRuntimeNodeView select(List<OcrRuntimeNodeView> nodes) {
        int totalWeight = nodes.stream().mapToInt(OcrRuntimeNodeView::weight).sum();
        OcrRuntimeNodeView selected = nodes.getFirst();
        int topWeight = Integer.MIN_VALUE;
        for (OcrRuntimeNodeView node : nodes) {
            int nextWeight = currentWeights.getOrDefault(node.nodeId(), 0) + node.weight();
            currentWeights.put(node.nodeId(), nextWeight);
            if (nextWeight >= topWeight) {
                selected = node;
                topWeight = nextWeight;
            } else {
                // 当前节点累计权重未超过已选最大值，继续比较后续节点。
            }
        }
        currentWeights.computeIfPresent(selected.nodeId(), (nodeId, value) -> value - totalWeight);
        return selected;
    }
}
