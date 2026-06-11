package io.github.lvdaxianer.doclens.j.query.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrBatchHitTracker;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrBatchNodeHit;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCall;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutingMode;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrRuntimeNodePool;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OCR Dashboard 指标测试共享支持。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
abstract class OcrDashboardMetricsProviderTestSupport {

    /*
     * 共享 support 只放无业务断言的测试基础设施。
     * 断言留在具体测试类中，保证失败信息仍然指向业务场景。
     *
     * 这些内存仓储模拟查询侧只读依赖，
     * save 类写接口保留显式异常，避免测试误用写路径。
     */

    /** 测试基准时间。 */
    protected static final OffsetDateTime BASE_TIME = OffsetDateTime.now()
            .withHour(10)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);
    /** 默认 OCR 模型键。 */
    protected static final String DEFAULT_MODEL_KEY = "paddle_ocr";
    /** 财务节点 ID。 */
    protected static final String FINANCE_NODE_ID = "ocr_node_1";
    /** 财务节点名称。 */
    protected static final String FINANCE_NODE_NAME = "财务 OCR 节点";
    /** 票据节点 ID。 */
    protected static final String INVOICE_NODE_ID = "ocr_node_2";
    /** 票据节点名称。 */
    protected static final String INVOICE_NODE_NAME = "票据 OCR 节点";
    /** 合同节点 ID。 */
    protected static final String CONTRACT_NODE_ID = "ocr_node_3";
    /** 合同节点名称。 */
    protected static final String CONTRACT_NODE_NAME = "合同 OCR 节点";
    /** 内存节点仓储初始容量。 */
    private static final int TEST_NODE_CAPACITY = 8;

    /**
     * 创建待测指标提供器。
     *
     * @param nodeRepository 节点仓储
     * @param callRepository 调用记录仓储
     * @return 指标提供器
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    protected OcrDashboardMetricsProvider provider(
            InMemoryOcrNodeRepository nodeRepository,
            InMemoryOcrNodeCallRepository callRepository
    ) {
        return provider(nodeRepository, callRepository, new InMemoryBatchHitTracker(List.of()));
    }

    /**
     * 创建带运行时命中跟踪的待测指标提供器。
     *
     * @param nodeRepository 节点仓储
     * @param callRepository 调用记录仓储
     * @param batchHitTracker 批次运行时命中跟踪器
     * @return 指标提供器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    protected OcrDashboardMetricsProvider provider(
            InMemoryOcrNodeRepository nodeRepository,
            InMemoryOcrNodeCallRepository callRepository,
            OcrBatchHitTracker batchHitTracker
    ) {
        /*
         * Provider 依赖运行时节点池提供节点快照。
         * 每个测试单独初始化节点池，避免跨测试共享状态。
         */
        OcrRuntimeNodePool nodePool = new OcrRuntimeNodePool(nodeRepository);
        nodePool.initialize();
        return new OcrDashboardMetricsProvider(nodeRepository, callRepository, nodePool,
                new OcrDashboardMetricsProvider.DashboardThreadPools(null, null, null, null), batchHitTracker);
    }

    /**
     * 创建离线测试节点。
     *
     * @param nodeId 节点 ID
     * @param name 节点名称
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    protected OcrNode offlineNode(String nodeId, String name) {
        /*
         * Dashboard 资源指标只关心节点可用性和基础容量。
         * 熔断、冷却和错误计数在这些聚合测试中保持空值。
         */
        return new OcrNode(nodeId, DEFAULT_MODEL_KEY, name, "10.0.0.1", 8080, true, true, 100, 4, OcrNodeStatus.UP,
                0L, 0L, 0L, 0L, Optional.of(BASE_TIME), Optional.empty(), Optional.empty(), Optional.empty(),
                BASE_TIME, BASE_TIME);
    }

    /**
     * 创建成功调用记录。
     *
     * @param callSeed 调用基础信息
     * @param nodeId 节点 ID
     * @param elapsedMs 耗时
     * @return 调用记录
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    protected OcrNodeCall successfulCall(CallSeed callSeed, String nodeId, long elapsedMs) {
        return new OcrNodeCall(callSeed.callId(), callSeed.batchId(), callSeed.documentId(), callSeed.pageNo(),
                DEFAULT_MODEL_KEY, nodeId, OcrRoutingMode.GLOBAL_LOAD_BALANCE, OcrNodeCallStatus.SUCCESS,
                0, elapsedMs, Optional.empty(), Optional.empty(), BASE_TIME, Optional.of(BASE_TIME.plusSeconds(1)));
    }

    /**
     * 创建失败调用记录。
     *
     * @param callSeed 调用基础信息
     * @param nodeId 节点 ID
     * @param elapsedMs 耗时
     * @return 失败调用记录
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    protected OcrNodeCall failedCall(CallSeed callSeed, String nodeId, long elapsedMs) {
        return new OcrNodeCall(callSeed.callId(), callSeed.batchId(), callSeed.documentId(), callSeed.pageNo(),
                DEFAULT_MODEL_KEY, nodeId, OcrRoutingMode.GLOBAL_LOAD_BALANCE, OcrNodeCallStatus.FAILED,
                1, elapsedMs, Optional.of("OCR_FAILED"), Optional.of("recognize failed"), BASE_TIME,
                Optional.of(BASE_TIME.plusSeconds(1)));
    }

    /**
     * 创建测试调用基础信息。
     *
     * @param callId 调用记录 ID
     * @param batchId 批次 ID
     * @param documentId 文档 ID
     * @param pageNo 页码
     * @return 调用基础信息
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    protected CallSeed callSeed(String callId, String batchId, String documentId, int pageNo) {
        return new CallSeed(callId, batchId, documentId, pageNo);
    }

    /**
     * 测试调用基础信息。
     *
     * @param callId 调用记录 ID
     * @param batchId 批次 ID
     * @param documentId 文档 ID
     * @param pageNo 页码
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    protected record CallSeed(String callId, String batchId, String documentId, int pageNo) {
    }

    /**
     * 内存节点仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    protected static final class InMemoryOcrNodeRepository implements OcrNodeRepository {

        private final Map<String, OcrNode> nodes = new HashMap<>(TEST_NODE_CAPACITY);

        /**
         * 创建内存节点仓储。
         *
         * @param seedNodes 初始节点
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        InMemoryOcrNodeRepository(List<OcrNode> seedNodes) {
            // 节点按 ID 建索引，匹配真实仓储的主键查询语义。
            seedNodes.forEach(node -> nodes.put(node.id(), node));
        }

        @Override
        public void save(OcrNode node) {
            nodes.put(node.id(), node);
        }

        @Override
        public void saveAll(List<OcrNode> seedNodes) {
            seedNodes.forEach(this::save);
        }

        @Override
        public void update(OcrNode node) {
            nodes.put(node.id(), node);
        }

        @Override
        public Optional<OcrNode> findById(String nodeId) {
            return Optional.ofNullable(nodes.get(nodeId));
        }

        @Override
        public Optional<OcrNode> findByModelHostPort(String modelKey, String host, int port) {
            // host/port 唯一性查询仅在节点池初始化路径中使用。
            return nodes.values().stream()
                    .filter(node -> node.modelKey().equals(modelKey) && node.host().equals(host) && node.port() == port)
                    .findFirst();
        }

        @Override
        public List<OcrNode> listByModelKey(String modelKey) {
            return nodes.values().stream().filter(node -> node.modelKey().equals(modelKey)).toList();
        }

        @Override
        public List<OcrNode> listEnabled() {
            return nodes.values().stream().filter(OcrNode::enabled).toList();
        }

        @Override
        public List<OcrNode> listAll() {
            return List.copyOf(nodes.values());
        }

        @Override
        public void deleteById(String nodeId) {
            nodes.remove(nodeId);
        }
    }

    /**
     * 内存调用记录仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    protected static final class InMemoryOcrNodeCallRepository implements OcrNodeCallRepository {

        private final List<OcrNodeCall> calls;

        /**
         * 创建内存调用记录仓储。
         *
         * @param calls 初始调用记录
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        InMemoryOcrNodeCallRepository(List<OcrNodeCall> calls) {
            this.calls = List.copyOf(calls);
        }

        @Override
        public void save(OcrNodeCall call) {
            throw new UnsupportedOperationException("test repository is read only");
        }

        @Override
        public List<OcrNodeCall> listByDocumentId(String documentId) {
            return calls.stream().filter(call -> call.documentId().equals(documentId)).toList();
        }

        @Override
        public List<OcrNodeCall> listByBatchId(String batchId) {
            return calls.stream().filter(call -> call.batchId().equals(batchId)).toList();
        }

        @Override
        public List<OcrNodeCall> listRecentByNodeId(String nodeId, int limit) {
            return calls.stream().filter(call -> call.nodeId().equals(nodeId)).limit(limit).toList();
        }

        @Override
        public List<OcrNodeCall> listByNodeIdsAndDay(List<String> nodeIds, LocalDate day) {
            // 资源指标按节点集合和自然日批量查询，测试仓储在内存中等价过滤。
            return calls.stream()
                    .filter(call -> nodeIds.contains(call.nodeId()) && call.startedAt().toLocalDate().equals(day))
                    .toList();
        }
    }

    /**
     * 内存批次运行时命中跟踪器。
     *
     * @param hits 命中快照
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    protected record InMemoryBatchHitTracker(List<OcrBatchNodeHit> hits) implements OcrBatchHitTracker {

        @Override
        public void recordDispatch(String batchId, String modelKey, String nodeId) {
            throw new UnsupportedOperationException("test tracker is read only");
        }

        @Override
        public void recordCompletion(String batchId, String modelKey, String nodeId) {
            throw new UnsupportedOperationException("test tracker is read only");
        }

        @Override
        public List<OcrBatchNodeHit> snapshotByBatch(String batchId) {
            // 运行时命中快照只暴露当前批次，模拟生产 tracker 的批次隔离。
            return hits.stream().filter(hit -> hit.batchId().equals(batchId)).toList();
        }
    }
}
