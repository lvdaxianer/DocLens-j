package io.github.lvdaxianer.doclens.j.adapter.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * OCR 派发协调器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class OcrDispatchCoordinatorTest {

    /**
     * 所有匹配节点满载时请求应进入等待队列。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void requestEntersWaitQueueWhenAllMatchingNodesAreFull() {
        FakePendingQueue queue = new FakePendingQueue();
        FakeRuntimeNodeProvider nodeProvider = fullNodes("node-a", "node-b");
        OcrDispatchCoordinator coordinator = coordinator(nodeProvider, queue);

        OcrDispatchAcquireResult result = coordinator.acquire(sampleRequest(),
                OcrRoutePolicy.globalLoadBalance("weighted-idle"));

        assertThat(result.queued()).isTrue();
        assertThat(queue.size()).isEqualTo(1);
    }

    /**
     * 等待队列满载时请求应稳定失败而不是继续留存。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    @Test
    void acquireFailsWithStableErrorWhenPendingQueueIsFull() {
        FakePendingQueue queue = new FakePendingQueue(1);
        FakeRuntimeNodeProvider nodeProvider = fullNodes("node-a", "node-b");
        OcrDispatchCoordinator coordinator = coordinator(nodeProvider, queue);

        coordinator.acquire(sampleRequest(), OcrRoutePolicy.globalLoadBalance("weighted-idle"));
        OcrDispatchAcquireResult result = coordinator.acquire(sampleRequest("doc-2"),
                OcrRoutePolicy.globalLoadBalance("weighted-idle"));

        assertThat(result.queued()).isFalse();
        assertThat(queue.size()).isEqualTo(1);
        assertThatThrownBy(result::awaitDispatch)
                .isInstanceOf(OcrRouteExecutionException.class)
                .hasMessageContaining("ocr dispatch queue is full");
    }

    /**
     * 释放槽位后应触发排队请求出队并重新派发。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void releaseTriggersDrainAndDispatchesQueuedRequest() {
        FakePendingQueue queue = new FakePendingQueue();
        FakeRuntimeNodeProvider nodeProvider = oneNodeAtCapacityThenFree("node-a", 1);
        OcrDispatchCoordinator coordinator = coordinator(nodeProvider, queue);

        coordinator.acquire(sampleRequest(), OcrRoutePolicy.globalLoadBalance("weighted-idle"));
        coordinator.acquire(sampleRequest("doc-2"), OcrRoutePolicy.globalLoadBalance("weighted-idle"));

        coordinator.release("node-a");

        assertThat(queue.size()).isEqualTo(0);
        assertThat(nodeProvider.snapshot().getFirst().inflightImages()).isEqualTo(1);
    }

    /**
     * 选中节点在占槽瞬间失去最后槽位时应回退到其他可用节点。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void acquireFallsBackToAnotherAvailableNodeWhenSelectedNodeLosesLastSlot() {
        FakePendingQueue queue = new FakePendingQueue();
        FakeRuntimeNodeProvider nodeProvider = availableNodes("node-a", "node-b");
        nodeProvider.rejectNextAcquire("node-a");
        OcrDispatchCoordinator coordinator = coordinator(nodeProvider, queue);

        OcrDispatchAcquireResult result = coordinator.acquire(sampleRequest(),
                OcrRoutePolicy.globalLoadBalance("weighted-idle"));

        assertThat(result.queued()).isFalse();
        assertThat(result.node()).isPresent();
        assertThat(result.node().get().nodeId()).isEqualTo("node-b");
        assertThat(queue.size()).isEqualTo(0);
    }

    /**
     * 创建测试协调器。
     *
     * @param nodeProvider 运行时节点提供器
     * @param queue 待派发队列
     * @return 派发协调器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrDispatchCoordinator coordinator(FakeRuntimeNodeProvider nodeProvider, FakePendingQueue queue) {
        return new OcrDispatchCoordinator(nodeProvider, new WeightedCapacityOcrNodeSelector(0.7D, 0.3D, 0.15D),
                queue);
    }

    /**
     * 创建满载节点提供器。
     *
     * @param firstNodeId 第一个节点 ID
     * @param secondNodeId 第二个节点 ID
     * @return 节点提供器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private FakeRuntimeNodeProvider fullNodes(String firstNodeId, String secondNodeId) {
        return new FakeRuntimeNodeProvider(List.of(
                node(firstNodeId, 50, 1, 1, 0),
                node(secondNodeId, 50, 1, 1, 0)
        ));
    }

    /**
     * 创建单节点先满载后释放的节点提供器。
     *
     * @param nodeId 节点 ID
     * @param maxConcurrency 最大并发
     * @return 节点提供器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private FakeRuntimeNodeProvider oneNodeAtCapacityThenFree(String nodeId, int maxConcurrency) {
        return new FakeRuntimeNodeProvider(List.of(node(nodeId, 50, maxConcurrency, 0, maxConcurrency)));
    }

    /**
     * 创建可用节点提供器。
     *
     * @param firstNodeId 第一个节点 ID
     * @param secondNodeId 第二个节点 ID
     * @return 节点提供器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private FakeRuntimeNodeProvider availableNodes(String firstNodeId, String secondNodeId) {
        return new FakeRuntimeNodeProvider(List.of(
                node(firstNodeId, 60, 1, 0, 1),
                node(secondNodeId, 40, 1, 0, 1)
        ));
    }

    /**
     * 创建测试请求。
     *
     * @return 图片 OCR 请求
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ImageOcrRequest sampleRequest() {
        return sampleRequest("doc-1");
    }

    /**
     * 创建测试请求。
     *
     * @param documentId 文档 ID
     * @return 图片 OCR 请求
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ImageOcrRequest sampleRequest(String documentId) {
        return new ImageOcrRequest("batch-1", documentId, documentId + ".png", 1, "image".getBytes(),
                JsonPayload.empty());
    }

    /**
     * 创建运行时节点视图。
     *
     * @param nodeId 节点 ID
     * @param weight 权重
     * @param maxConcurrency 最大并发
     * @param inflightImages 正在执行数量
     * @param availableSlots 可用槽位数
     * @return 运行时节点视图
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrRuntimeNodeView node(
            String nodeId,
            int weight,
            int maxConcurrency,
            int inflightImages,
            int availableSlots
    ) {
        return new OcrRuntimeNodeView(nodeId, "paddle_ocr", true, true, OcrNodeStatus.UP, weight,
                maxConcurrency, inflightImages, 0, availableSlots, 100L, Optional.empty(), 0L, 0L);
    }

    /**
     * 假运行时节点提供器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class FakeRuntimeNodeProvider implements OcrRuntimeNodeProvider {

        private final List<OcrRuntimeNodeView> nodes;
        private final List<String> rejectedAcquireNodeIds = new ArrayList<>(1);

        private FakeRuntimeNodeProvider(List<OcrRuntimeNodeView> nodes) {
            this.nodes = new ArrayList<>(nodes);
        }

        @Override
        public List<OcrRuntimeNodeView> snapshot() {
            return List.copyOf(nodes);
        }

        @Override
        public Optional<OcrRuntimeNodeView> incrementInflight(String nodeId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<OcrRuntimeNodeView> decrementInflight(String nodeId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<OcrRuntimeNodeView> tryAcquireSlot(String nodeId) {
            if (rejectedAcquireNodeIds.remove(nodeId)) {
                update(nodeId, 1, -1);
                return Optional.empty();
            } else {
                return update(nodeId, 1, -1);
            }
        }

        @Override
        public Optional<OcrRuntimeNodeView> releaseSlot(String nodeId) {
            return update(nodeId, -1, 1);
        }

        @Override
        public Optional<OcrRuntimeNodeView> incrementQueued(String nodeId) {
            return Optional.empty();
        }

        @Override
        public Optional<OcrRuntimeNodeView> decrementQueued(String nodeId) {
            return Optional.empty();
        }

        /**
         * 标记节点下一次占槽应模拟失败。
         *
         * @param nodeId 节点 ID
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        void rejectNextAcquire(String nodeId) {
            rejectedAcquireNodeIds.add(nodeId);
        }

        /**
         * 更新节点的 inflight 和 availableSlots。
         *
         * @param nodeId 节点 ID
         * @param inflightDelta inflight 变更量
         * @param slotDelta 槽位变更量
         * @return 更新后的节点
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        private Optional<OcrRuntimeNodeView> update(String nodeId, int inflightDelta, int slotDelta) {
            for (int index = 0; index < nodes.size(); index++) {
                OcrRuntimeNodeView node = nodes.get(index);
                if (node.nodeId().equals(nodeId) && node.availableSlots() + slotDelta >= 0) {
                    OcrRuntimeNodeView updated = new OcrRuntimeNodeView(node.nodeId(), node.modelKey(),
                            node.enabled(), node.participateGlobal(), node.status(), node.weight(),
                            node.maxConcurrency(), node.inflightImages() + inflightDelta, node.queuedImages(),
                            node.availableSlots() + slotDelta, node.avgLatencyMs(), node.circuitOpenUntil(),
                            node.consecutiveFailureCount(), node.recoverySuccessCount());
                    nodes.set(index, updated);
                    return Optional.of(updated);
                } else {
                    // 节点不匹配或无可用槽位时继续检查下一个节点。
                }
            }
            return Optional.empty();
        }
    }

    /**
     * 假待派发队列。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class FakePendingQueue implements OcrPendingRequestQueue {

        private final ArrayDeque<OcrPendingRequest> requests = new ArrayDeque<>();
        private final int capacity;

        private FakePendingQueue() { this(Integer.MAX_VALUE); }
        private FakePendingQueue(int capacity) { this.capacity = capacity; }

        @Override
        public boolean enqueue(OcrPendingRequest request) {
            if (requests.size() < capacity) {
                requests.addLast(request);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public Optional<OcrPendingRequest> poll() {
            return Optional.ofNullable(requests.pollFirst());
        }

        /**
         * 返回队列长度。
         *
         * @return 队列长度
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        int size() {
            return requests.size();
        }
    }
}
