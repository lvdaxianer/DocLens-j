package io.github.lvdaxianer.doclens.j.query.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCall;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCreateRequest;
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
import org.junit.jupiter.api.Test;

/**
 * OCR Dashboard 指标提供器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class OcrDashboardMetricsProviderTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.now()
            .withHour(10)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);

    /**
     * 批次命中节点应返回节点别名，避免前端退回显示内部 ID。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void hitNodesByBatchIncludesNodeName() {
        InMemoryOcrNodeRepository nodeRepository = new InMemoryOcrNodeRepository(List.of(
                offlineNode("ocr_node_1", "财务 OCR 节点")
        ));
        InMemoryOcrNodeCallRepository callRepository = new InMemoryOcrNodeCallRepository(List.of(
                successfulCall("call-1", "batch-1", "doc-1", "paddle_ocr", "ocr_node_1", 320),
                successfulCall("call-2", "batch-1", "doc-1", "paddle_ocr", "ocr_node_1", 480)
        ));
        OcrDashboardMetricsProvider provider = provider(nodeRepository, callRepository);

        List<Map<String, Object>> hitNodes = provider.hitNodesByBatch("batch-1");

        assertThat(hitNodes).singleElement().satisfies(row -> assertThat(row)
                .containsEntry("node_id", "ocr_node_1")
                .containsEntry("node_name", "财务 OCR 节点")
                .containsEntry("image_count", 2L));
    }

    /**
     * OCR 资源指标应基于调用记录计算今日处理、平均耗时和 P95。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void ocrResourcesIncludesLatencyMetricsFromCalls() {
        InMemoryOcrNodeRepository nodeRepository = new InMemoryOcrNodeRepository(List.of(
                offlineNode("ocr_node_1", "财务 OCR 节点"),
                offlineNode("ocr_node_2", "票据 OCR 节点")
        ));
        InMemoryOcrNodeCallRepository callRepository = new InMemoryOcrNodeCallRepository(List.of(
                successfulCall("call-1", "batch-1", "doc-1", "paddle_ocr", "ocr_node_1", 100),
                successfulCall("call-2", "batch-1", "doc-1", "paddle_ocr", "ocr_node_1", 200),
                successfulCall("call-3", "batch-2", "doc-2", "paddle_ocr", "ocr_node_2", 600)
        ));
        OcrDashboardMetricsProvider provider = provider(nodeRepository, callRepository);

        Map<String, Object> resources = provider.ocrResources();

        assertThat(resources).containsEntry("healthy_node_count", 2L);
        assertThat(resources.get("nodes")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .anySatisfy(row -> assertThat(row).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                        .containsEntry("node_id", "ocr_node_1")
                        .containsEntry("node_name", "财务 OCR 节点")
                        .containsEntry("processed_images_today", 2L)
                        .containsEntry("avg_latency_ms", 150L)
                        .containsEntry("p95_latency_ms", 200L))
                .anySatisfy(row -> assertThat(row).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                        .containsEntry("node_id", "ocr_node_2")
                        .containsEntry("node_name", "票据 OCR 节点")
                        .containsEntry("processed_images_today", 1L)
                        .containsEntry("avg_latency_ms", 600L)
                        .containsEntry("p95_latency_ms", 600L));
    }

    /**
     * 创建待测指标提供器。
     *
     * @param nodeRepository 节点仓储
     * @param callRepository 调用记录仓储
     * @return 指标提供器
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrDashboardMetricsProvider provider(
            InMemoryOcrNodeRepository nodeRepository,
            InMemoryOcrNodeCallRepository callRepository
    ) {
        OcrRuntimeNodePool nodePool = new OcrRuntimeNodePool(nodeRepository);
        nodePool.initialize();
        return new OcrDashboardMetricsProvider(nodeRepository, callRepository, nodePool,
                new OcrDashboardMetricsProvider.DashboardThreadPools(null, null, null, null));
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
    private OcrNode offlineNode(String nodeId, String name) {
        return new OcrNode(nodeId, "paddle_ocr", name, "10.0.0.1", 8080, true, true, 100, 4, OcrNodeStatus.UP,
                0L, 0L, 0L, 0L, Optional.of(BASE_TIME), Optional.empty(), Optional.empty(), Optional.empty(),
                BASE_TIME, BASE_TIME);
    }

    /**
     * 创建成功调用记录。
     *
     * @param callId 调用记录 ID
     * @param batchId 批次 ID
     * @param documentId 文档 ID
     * @param modelKey 模型标识
     * @param nodeId 节点 ID
     * @param elapsedMs 耗时
     * @return 调用记录
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNodeCall successfulCall(
            String callId,
            String batchId,
            String documentId,
            String modelKey,
            String nodeId,
            long elapsedMs
    ) {
        return new OcrNodeCall(callId, batchId, documentId, 1, modelKey, nodeId, OcrRoutingMode.GLOBAL_LOAD_BALANCE,
                OcrNodeCallStatus.SUCCESS, 0, elapsedMs, Optional.empty(), Optional.empty(), BASE_TIME,
                Optional.of(BASE_TIME.plusSeconds(1)));
    }

    /**
     * 内存节点仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static final class InMemoryOcrNodeRepository implements OcrNodeRepository {

        private final Map<String, OcrNode> nodes = new HashMap<>();

        private InMemoryOcrNodeRepository(List<OcrNode> seedNodes) {
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
    private static final class InMemoryOcrNodeCallRepository implements OcrNodeCallRepository {

        private final List<OcrNodeCall> calls;

        private InMemoryOcrNodeCallRepository(List<OcrNodeCall> calls) {
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
            return calls.stream()
                    .filter(call -> nodeIds.contains(call.nodeId()) && call.startedAt().toLocalDate().equals(day))
                    .toList();
        }
    }
}
