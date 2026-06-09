package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutingMode;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * OCR 同步派发协调器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public final class OcrDispatchCoordinator {

    private static final int INITIAL_ATTEMPTED_NODE_CAPACITY = 4;

    private final OcrRuntimeNodeProvider nodeProvider;
    private final OcrNodeSelector selector;
    private final OcrPendingRequestQueue queue;

    /**
     * 创建 OCR 同步派发协调器。
     *
     * @param nodeProvider 运行时节点提供器
     * @param selector OCR 节点选择器
     * @param queue 待派发请求队列
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrDispatchCoordinator(
            OcrRuntimeNodeProvider nodeProvider,
            OcrNodeSelector selector,
            OcrPendingRequestQueue queue
    ) {
        this.nodeProvider = nodeProvider;
        this.selector = selector;
        this.queue = queue;
    }

    /**
     * 尝试为请求占用一个节点槽位。
     *
     * @param request 图片 OCR 请求
     * @param policy OCR 路由策略
     * @return 占槽结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrDispatchAcquireResult acquire(ImageOcrRequest request, OcrRoutePolicy policy) {
        return acquire(request, policy, Set.of());
    }

    /**
     * 尝试为请求占用一个未被排除节点的槽位。
     *
     * @param request 图片 OCR 请求
     * @param policy OCR 路由策略
     * @param excludedNodeIds 已排除节点
     * @return 占槽结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrDispatchAcquireResult acquire(
            ImageOcrRequest request,
            OcrRoutePolicy policy,
            Set<String> excludedNodeIds
    ) {
        if (!hasMatchingNode(policy, excludedNodeIds)) {
            return OcrDispatchAcquireResult.unavailable();
        }
        Optional<OcrRuntimeNodeView> acquired = tryAcquireAvailableNode(policy, excludedNodeIds);
        if (acquired.isPresent()) {
            return OcrDispatchAcquireResult.dispatched(acquired.get());
        } else {
            // 当前没有可用节点或候选节点在竞争中全部失去槽位，转入等待队列。
        }
        CompletableFuture<OcrRuntimeNodeView> dispatchFuture = new CompletableFuture<>();
        queue.enqueue(OcrPendingRequest.from(request, policy, excludedNodeIds, dispatchFuture));
        return OcrDispatchAcquireResult.queuedResult(dispatchFuture);
    }

    /**
     * 释放节点槽位并尝试派发一个排队请求。
     *
     * @param nodeId 节点 ID
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public void release(String nodeId) {
        nodeProvider.releaseSlot(nodeId);
        drainOne();
    }

    /**
     * 尝试派发一个排队请求。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void drainOne() {
        Optional<OcrPendingRequest> pendingRequest = queue.poll();
        if (pendingRequest.isPresent()) {
            OcrPendingRequest request = pendingRequest.get();
            Optional<OcrRuntimeNodeView> acquired = tryAcquireAvailableNode(request.policy(), request.excludedNodeIds());
            if (acquired.isPresent()) {
                request.dispatchFuture().complete(acquired.get());
                return;
            } else {
                // 当前仍无可派发节点或候选节点在竞争中全部失去槽位，请求重新入队等待下一次机会。
            }
            queue.enqueue(request);
        } else {
            // 当前没有排队请求，无需继续派发。
        }
    }

    /**
     * 尝试从当前可选节点中占用一个槽位。
     *
     * @param policy OCR 路由策略
     * @return 成功占槽的节点
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private Optional<OcrRuntimeNodeView> tryAcquireAvailableNode(OcrRoutePolicy policy, Set<String> excludedNodeIds) {
        Set<String> attemptedNodeIds = new LinkedHashSet<>(INITIAL_ATTEMPTED_NODE_CAPACITY);
        while (true) {
            Optional<OcrRuntimeNodeView> selected = selectUntriedNode(policy, excludedNodeIds, attemptedNodeIds);
            if (selected.isEmpty()) {
                return Optional.empty();
            } else if (attemptedNodeIds.add(selected.get().nodeId())) {
                Optional<OcrRuntimeNodeView> acquired = nodeProvider.tryAcquireSlot(selected.get().nodeId());
                if (acquired.isPresent()) {
                    return acquired;
                } else {
                    // 选中节点在占槽瞬间失去最后槽位时，继续尝试其他候选节点。
                }
            } else {
                // 已尝试过该节点时继续下一轮，避免重复占槽同一个失效候选。
            }
        }
    }

    /**
     * 选择一个尚未尝试过的候选节点。
     *
     * @param policy OCR 路由策略
     * @param attemptedNodeIds 已尝试节点集合
     * @return 尚未尝试的候选节点
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private Optional<OcrRuntimeNodeView> selectUntriedNode(
            OcrRoutePolicy policy,
            Set<String> excludedNodeIds,
            Set<String> attemptedNodeIds
    ) {
        return selector.select(policy, candidateNodes(policy, excludedNodeIds).stream()
                .filter(node -> !attemptedNodeIds.contains(node.nodeId()))
                .toList());
    }

    /**
     * 判断是否仍存在匹配路由策略且未被排除的节点。
     *
     * @param policy OCR 路由策略
     * @param excludedNodeIds 已排除节点
     * @return 是否存在匹配节点
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private boolean hasMatchingNode(OcrRoutePolicy policy, Set<String> excludedNodeIds) {
        return !candidateNodes(policy, excludedNodeIds).isEmpty();
    }

    /**
     * 过滤出匹配路由策略且未排除的候选节点。
     *
     * @param policy OCR 路由策略
     * @param excludedNodeIds 已排除节点
     * @return 候选节点集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private List<OcrRuntimeNodeView> candidateNodes(OcrRoutePolicy policy, Set<String> excludedNodeIds) {
        return nodeProvider.snapshot().stream()
                .filter(node -> !excludedNodeIds.contains(node.nodeId()))
                .filter(OcrRuntimeNodeView::enabled)
                .filter(node -> node.status() == OcrNodeStatus.UP)
                .filter(node -> matchesPolicy(policy, node))
                .toList();
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
     * 判断非模型路由模式下的节点是否匹配。
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
