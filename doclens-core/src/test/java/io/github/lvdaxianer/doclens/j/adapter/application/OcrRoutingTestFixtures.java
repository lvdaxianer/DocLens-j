package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCall;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OCR 路由测试夹具。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
final class OcrRoutingTestFixtures {

    /*
     * 本夹具只保留 OCR 路由测试的基础依赖：
     * 运行时节点提供器、节点执行器、调用记录仓储和固定 ID 生成器。
     * 批次命中跟踪器已经拆到 OcrRoutingHitTestFixtures，
     * 避免一个 fixture 同时承载路由基础设施和运行态统计职责。
     */

    static final int TEST_CALL_CAPACITY = 8;

    private OcrRoutingTestFixtures() {
    }

    /**
     * 内存运行时节点提供器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    static class InMemoryRuntimeNodeProvider implements OcrRuntimeNodeProvider {

        /*
         * 这个内存实现模拟生产节点运行态：
         * nodes 保存静态节点配置，inflightImages 和 queuedImages 保存动态计数。
         * 测试通过 tryAcquireCount / releaseCount 验证调度器是否走了协调器路径。
         */

        private final Map<String, OcrRuntimeNodeView> nodes = new HashMap<>(TEST_CALL_CAPACITY);
        private final Map<String, Integer> inflightImages = new HashMap<>(TEST_CALL_CAPACITY);
        private final Map<String, Integer> queuedImages = new HashMap<>(TEST_CALL_CAPACITY);
        private int tryAcquireCount;
        private int releaseCount;

        /**
         * 创建内存运行时节点提供器。
         *
         * @param nodes 运行时节点集合
         * @author lvdaxianerplus
         * @date 2026-06-08
         */
        InMemoryRuntimeNodeProvider(List<OcrRuntimeNodeView> nodes) {
            nodes.forEach(node -> {
                this.nodes.put(node.nodeId(), node);
                this.inflightImages.put(node.nodeId(), node.inflightImages());
                this.queuedImages.put(node.nodeId(), node.queuedImages());
            });
        }

        @Override
        public synchronized List<OcrRuntimeNodeView> snapshot() {
            return nodes.values().stream().map(this::withInflight).toList();
        }

        @Override
        public synchronized Optional<OcrRuntimeNodeView> incrementInflight(String nodeId) {
            return updateInflight(nodeId, 1);
        }

        @Override
        public synchronized Optional<OcrRuntimeNodeView> decrementInflight(String nodeId) {
            return updateInflight(nodeId, -1);
        }

        @Override
        public synchronized Optional<OcrRuntimeNodeView> tryAcquireSlot(String nodeId) {
            tryAcquireCount++;
            return Optional.ofNullable(nodes.get(nodeId))
                    .filter(node -> availableSlots(nodeId) > 0)
                    .flatMap(node -> updateInflight(nodeId, 1));
        }

        @Override
        public synchronized Optional<OcrRuntimeNodeView> releaseSlot(String nodeId) {
            releaseCount++;
            return updateInflight(nodeId, -1);
        }

        @Override
        public synchronized Optional<OcrRuntimeNodeView> incrementQueued(String nodeId) {
            return updateQueued(nodeId, 1);
        }

        @Override
        public synchronized Optional<OcrRuntimeNodeView> decrementQueued(String nodeId) {
            return updateQueued(nodeId, -1);
        }

        /**
         * 查询节点解析中图片数。
         *
         * @param nodeId 节点 ID
         * @return 解析中图片数
         * @author lvdaxianerplus
         * @date 2026-06-08
         */
        synchronized int inflight(String nodeId) {
            return inflightImages.getOrDefault(nodeId, 0);
        }

        /**
         * 返回占槽尝试次数。
         *
         * @return 占槽尝试次数
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        synchronized int tryAcquireCount() {
            return tryAcquireCount;
        }

        /**
         * 返回释放槽位次数。
         *
         * @return 释放槽位次数
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        synchronized int releaseCount() {
            return releaseCount;
        }

        /**
         * 更新节点解析中图片数。
         *
         * @param nodeId 节点 ID
         * @param delta 增量
         * @return 更新后的节点视图
         * @author lvdaxianerplus
         * @date 2026-06-08
         */
        private Optional<OcrRuntimeNodeView> updateInflight(String nodeId, int delta) {
            return Optional.ofNullable(nodes.get(nodeId)).map(node -> {
                Integer currentValue = Optional.ofNullable(inflightImages.get(nodeId)).orElse(0);
                inflightImages.put(nodeId, Math.max(0, currentValue + delta));
                return withInflight(node);
            });
        }

        /**
         * 更新节点排队图片数。
         *
         * @param nodeId 节点 ID
         * @param delta 增量
         * @return 更新后的节点视图
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        private Optional<OcrRuntimeNodeView> updateQueued(String nodeId, int delta) {
            return Optional.ofNullable(nodes.get(nodeId)).map(node -> {
                Integer currentValue = Optional.ofNullable(queuedImages.get(nodeId)).orElse(0);
                queuedImages.put(nodeId, Math.max(0, currentValue + delta));
                return withInflight(node);
            });
        }

        /**
         * 使用最新解析中图片数重建节点视图。
         *
         * @param node 节点视图
         * @return 更新后的节点视图
         * @author lvdaxianerplus
         * @date 2026-06-08
         */
        private OcrRuntimeNodeView withInflight(OcrRuntimeNodeView node) {
            int inflight = inflightImages.getOrDefault(node.nodeId(), 0);
            int queued = queuedImages.getOrDefault(node.nodeId(), 0);
            return new OcrRuntimeNodeView(node.nodeId(), node.modelKey(), node.enabled(), node.participateGlobal(),
                    node.status(), node.weight(), node.maxConcurrency(), inflight, queued,
                    availableSlots(node.nodeId()), node.avgLatencyMs(), node.circuitOpenUntil(),
                    node.consecutiveFailureCount(), node.recoverySuccessCount());
        }

        /**
         * 计算节点当前可用槽位。
         *
         * @param nodeId 节点 ID
         * @return 可用槽位数
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        private int availableSlots(String nodeId) {
            OcrRuntimeNodeView node = nodes.get(nodeId);
            int inflight = inflightImages.getOrDefault(nodeId, 0);
            return Math.max(0, node.maxConcurrency() - inflight);
        }
    }

    /**
     * 记录型节点 OCR 执行器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    static class RecordingNodeExecutor implements OcrNodeImageExecutor {

        private final Map<String, Integer> attempts = new HashMap<>(TEST_CALL_CAPACITY);
        private final Map<String, Integer> failures = new HashMap<>(TEST_CALL_CAPACITY);

        @Override
        public ImageOcrResult recognize(OcrRuntimeNodeView node, ImageOcrRequest request) {
            attempts.merge(node.nodeId(), 1, Integer::sum);
            if (attempts(node.nodeId()) <= failures.getOrDefault(node.nodeId(), 0)) {
                throw new IllegalStateException("node failed: " + node.nodeId());
            } else {
                return ImageOcrResult.fromBlocks(request.pageNo(), Map.of(), List.of(), List.of());
            }
        }

        /**
         * 设置节点失败次数。
         *
         * @param nodeId 节点 ID
         * @param times 失败次数
         * @author lvdaxianerplus
         * @date 2026-06-08
         */
        void fail(String nodeId, int times) {
            failures.put(nodeId, times);
        }

        /**
         * 查询节点尝试次数。
         *
         * @param nodeId 节点 ID
         * @return 尝试次数
         * @author lvdaxianerplus
         * @date 2026-06-08
         */
        int attempts(String nodeId) {
            return attempts.getOrDefault(nodeId, 0);
        }
    }

    /**
     * 内存 OCR 调用记录仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    static class InMemoryCallRepository implements OcrNodeCallRepository {

        /*
         * 调用记录仓储只做内存列表过滤。
         * 这里不模拟数据库分页或索引，
         * 目的是让路由测试聚焦“是否记录调用”和“记录归属是否正确”。
         * 所有查询都保持无副作用，方便断言路由行为。
         */

        final List<OcrNodeCall> calls = new ArrayList<>(TEST_CALL_CAPACITY);

        @Override
        public void save(OcrNodeCall call) {
            calls.add(call);
        }

        @Override
        public List<OcrNodeCall> listByDocumentId(String documentId) {
            return calls.stream().filter(call -> documentId.equals(call.documentId())).toList();
        }

        @Override
        public List<OcrNodeCall> listByBatchId(String batchId) {
            return calls.stream().filter(call -> batchId.equals(call.batchId())).toList();
        }

        @Override
        public List<OcrNodeCall> listRecentByNodeId(String nodeId, int limit) {
            return calls.stream().filter(call -> nodeId.equals(call.nodeId())).limit(limit).toList();
        }

        @Override
        public List<OcrNodeCall> listByNodeIdsAndDay(List<String> nodeIds, LocalDate day) {
            return calls.stream()
                    .filter(call -> nodeIds.contains(call.nodeId()) && call.startedAt().toLocalDate().equals(day))
                    .toList();
        }
    }

    /**
     * 固定 OCR 调用记录 ID 生成器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    static class FixedCallIdGenerator implements OcrCallIdGenerator {

        private int sequence;

        @Override
        public String newOcrCallId() {
            sequence++;
            return "call-" + sequence;
        }
    }
}
