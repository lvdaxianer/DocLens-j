package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCall;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OCR 路由测试夹具。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
final class OcrRoutingTestFixtures {

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
        public List<OcrRuntimeNodeView> snapshot() {
            return nodes.values().stream().map(this::withInflight).toList();
        }

        @Override
        public Optional<OcrRuntimeNodeView> incrementInflight(String nodeId) {
            return updateInflight(nodeId, 1);
        }

        @Override
        public Optional<OcrRuntimeNodeView> decrementInflight(String nodeId) {
            return updateInflight(nodeId, -1);
        }

        @Override
        public Optional<OcrRuntimeNodeView> tryAcquireSlot(String nodeId) {
            tryAcquireCount++;
            return Optional.ofNullable(nodes.get(nodeId))
                    .filter(node -> availableSlots(nodeId) > 0)
                    .flatMap(node -> updateInflight(nodeId, 1));
        }

        @Override
        public Optional<OcrRuntimeNodeView> releaseSlot(String nodeId) {
            releaseCount++;
            return updateInflight(nodeId, -1);
        }

        @Override
        public Optional<OcrRuntimeNodeView> incrementQueued(String nodeId) {
            return updateQueued(nodeId, 1);
        }

        @Override
        public Optional<OcrRuntimeNodeView> decrementQueued(String nodeId) {
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
        int inflight(String nodeId) {
            return inflightImages.getOrDefault(nodeId, 0);
        }

        /**
         * 返回占槽尝试次数。
         *
         * @return 占槽尝试次数
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        int tryAcquireCount() {
            return tryAcquireCount;
        }

        /**
         * 返回释放槽位次数。
         *
         * @return 释放槽位次数
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        int releaseCount() {
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
     * 内存批次运行时命中跟踪器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    static class InMemoryBatchHitTracker implements OcrBatchHitTracker {

        private static final int KEY_SEGMENT_COUNT = 3;
        private static final String KEY_SEPARATOR = "|";
        private final Map<String, AtomicLong> hitCounts = new ConcurrentHashMap<>(TEST_CALL_CAPACITY);

        @Override
        public void recordDispatch(String batchId, String modelKey, String nodeId) {
            hitCounts.computeIfAbsent(hitKey(batchId, modelKey, nodeId), ignored -> new AtomicLong(0L))
                    .incrementAndGet();
        }

        @Override
        public void recordCompletion(String batchId, String modelKey, String nodeId) {
            String key = hitKey(batchId, modelKey, nodeId);
            AtomicLong counter = hitCounts.get(key);
            if (counter == null) {
                // 测试中未记录派发时收到完成回调，直接忽略避免负计数。
                return;
            }
            long currentValue = counter.decrementAndGet();
            if (currentValue <= 0L) {
                // 保持与生产实现一致：归零但不删除计数器。
                counter.set(0L);
            }
        }

        @Override
        public List<OcrBatchNodeHit> snapshotByBatch(String batchId) {
            return hitCounts.entrySet().stream()
                    .filter(entry -> entry.getValue().get() > 0L)
                    .map(entry -> toHit(entry.getKey(), entry.getValue().get()))
                    .filter(hit -> batchId.equals(hit.batchId()))
                    .sorted(Comparator.comparing(OcrBatchNodeHit::nodeId))
                    .toList();
        }

        /**
         * 组装批次命中键，避免维护多层并发结构。
         *
         * @param batchId 批次 ID
         * @param modelKey 模型标识
         * @param nodeId 节点 ID
         * @return 命中键
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        private String hitKey(String batchId, String modelKey, String nodeId) {
            return batchId + KEY_SEPARATOR + modelKey + KEY_SEPARATOR + nodeId;
        }

        /**
         * 将命中键转换为快照对象。
         *
         * @param key 命中键
         * @param imageCount 图片数
         * @return 命中快照
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        private OcrBatchNodeHit toHit(String key, long imageCount) {
            String[] segments = key.split("\\|", KEY_SEGMENT_COUNT);
            return new OcrBatchNodeHit(segments[0], segments[1], segments[2], imageCount);
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
