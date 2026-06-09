package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

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
        Optional<OcrRuntimeNodeView> acquired = tryAcquireAvailableNode(policy);
        if (acquired.isPresent()) {
            return OcrDispatchAcquireResult.dispatched(acquired.get());
        } else {
            // 当前没有可用节点或候选节点在竞争中全部失去槽位，转入等待队列。
        }
        queue.enqueue(OcrPendingRequest.from(request, policy));
        return OcrDispatchAcquireResult.queuedResult();
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
            Optional<OcrRuntimeNodeView> acquired = tryAcquireAvailableNode(request.policy());
            if (acquired.isPresent()) {
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
    private Optional<OcrRuntimeNodeView> tryAcquireAvailableNode(OcrRoutePolicy policy) {
        Set<String> attemptedNodeIds = new LinkedHashSet<>(INITIAL_ATTEMPTED_NODE_CAPACITY);
        while (true) {
            Optional<OcrRuntimeNodeView> selected = selectUntriedNode(policy, attemptedNodeIds);
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
    private Optional<OcrRuntimeNodeView> selectUntriedNode(OcrRoutePolicy policy, Set<String> attemptedNodeIds) {
        return selector.select(policy, nodeProvider.snapshot().stream()
                .filter(node -> !attemptedNodeIds.contains(node.nodeId()))
                .toList());
    }
}
