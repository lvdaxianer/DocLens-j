package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 基于解析中图片数的 OCR 节点选择器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class LeastInflightOcrNodeSelector implements OcrNodeSelector {

    /**
     * 从候选节点中选择解析中图片数最少的节点。
     *
     * @param policy OCR 路由策略
     * @param nodes 候选节点
     * @return 选中的节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Override
    public Optional<OcrRuntimeNodeView> select(OcrRoutePolicy policy, List<OcrRuntimeNodeView> nodes) {
        return nodes.stream()
                .filter(node -> isSelectable(policy, node))
                .min(Comparator.comparingInt(OcrRuntimeNodeView::inflightImages)
                        .thenComparingLong(OcrRuntimeNodeView::avgLatencyMs)
                        .thenComparing(OcrRuntimeNodeView::nodeId));
    }

    /**
     * 判断节点是否可被当前策略选择。
     *
     * @param policy OCR 路由策略
     * @param node OCR 运行时节点
     * @return 是否可选择
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private boolean isSelectable(OcrRoutePolicy policy, OcrRuntimeNodeView node) {
        return node.enabled()
                && node.status() == OcrNodeStatus.UP
                && node.inflightImages() < node.maxConcurrency()
                && matchesPolicy(policy, node);
    }

    /**
     * 判断节点是否匹配路由策略。
     *
     * @param policy OCR 路由策略
     * @param node OCR 运行时节点
     * @return 是否匹配
     * @author lvdaxianerplus
     * @date 2026-06-08
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
     * @param node OCR 运行时节点
     * @return 是否匹配
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private boolean matchesNonModelPolicy(OcrRoutePolicy policy, OcrRuntimeNodeView node) {
        if (policy.routingMode() == OcrRoutingMode.SPECIFIC_NODE) {
            return policy.nodeId().filter(node.nodeId()::equals).isPresent();
        } else {
            return node.participateGlobal();
        }
    }
}
