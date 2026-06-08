package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCall;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
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
         * 使用最新解析中图片数重建节点视图。
         *
         * @param node 节点视图
         * @return 更新后的节点视图
         * @author lvdaxianerplus
         * @date 2026-06-08
         */
        private OcrRuntimeNodeView withInflight(OcrRuntimeNodeView node) {
            return new OcrRuntimeNodeView(node.nodeId(), node.modelKey(), node.enabled(), node.participateGlobal(),
                    node.status(), node.maxConcurrency(), inflightImages.getOrDefault(node.nodeId(), 0),
                    node.avgLatencyMs());
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
        public List<OcrNodeCall> listRecentByNodeId(String nodeId, int limit) {
            return calls.stream().filter(call -> nodeId.equals(call.nodeId())).limit(limit).toList();
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
