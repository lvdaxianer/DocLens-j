package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeManagementService;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCall;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeMetricsViewReader;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrModelDefinition;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrModelRegistry;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeMetrics;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * OCR 节点控制器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class OcrNodeControllerTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-09T10:00:00+08:00");
    private static final int PADDLE_DEFAULT_PORT = 18081;

    /**
     * 节点列表应返回基于调用记录聚合的真实耗时统计。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void listNodesReturnsMetricsFromAggregator() {
        List<OcrNode> nodes = List.of(
                offlineNode("ocr_node_1", "财务 OCR 节点"),
                offlineNode("ocr_node_2", "票据 OCR 节点")
        );
        OcrNodeManagementService managementService = new OcrNodeManagementService(
                new PassThroughModelRegistry(),
                new FixedNodeRepository(nodes),
                null,
                null
        );
        OcrNodeMetricsViewReader metricsViewReader = new FixedMetricsViewReader(Map.of(
                "ocr_node_1", new OcrNodeMetrics(0, 0, 2L, 2L, 0L, 150L, 200L, Optional.of(BASE_TIME), Optional.empty()),
                "ocr_node_2", new OcrNodeMetrics(0, 0, 1L, 1L, 0L, 600L, 600L, Optional.of(BASE_TIME), Optional.empty())
        ));

        OcrNodeController controller = new OcrNodeController(managementService, metricsViewReader,
                new EmptyCallRepository());

        Map<String, List<OcrNodeResponse>> response = controller.listNodes("paddle_ocr");

        assertThat(response.get("items")).extracting(OcrNodeResponse::id, OcrNodeResponse::processedImagesToday,
                        OcrNodeResponse::avgLatencyMs, OcrNodeResponse::p95LatencyMs)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("ocr_node_1", 2L, 150L, 200L),
                        org.assertj.core.groups.Tuple.tuple("ocr_node_2", 1L, 600L, 600L)
                );
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
                9L, 9L, 999L, 999L, Optional.of(BASE_TIME), Optional.empty(), Optional.empty(), Optional.empty(),
                BASE_TIME, BASE_TIME);
    }

    /**
     * 固定指标读取器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static final class FixedMetricsViewReader implements OcrNodeMetricsViewReader {

        private final Map<String, OcrNodeMetrics> metricsByNodeId;

        private FixedMetricsViewReader(Map<String, OcrNodeMetrics> metricsByNodeId) {
            this.metricsByNodeId = metricsByNodeId;
        }

        @Override
        public Map<String, OcrNodeMetrics> metricsByNodeIds(List<String> nodeIds) {
            return metricsByNodeId;
        }
    }

    /**
     * 透传模型注册表。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static final class PassThroughModelRegistry extends OcrModelRegistry {

        private PassThroughModelRegistry() {
            super(List.of(OcrModelDefinition.create(new OcrModelDefinition.CreateCommand(
                    new OcrModelDefinition.Identity("paddle_ocr", "PaddleOCR",
                            "PaddleOCR native-compatible HTTP API"),
                    new OcrModelDefinition.Capability(List.of("image"), "/ocr", "/ocr"),
                    new OcrModelDefinition.RuntimeDefaults(PADDLE_DEFAULT_PORT, "", "", true)))));
        }
    }

    /**
     * 空调用记录仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static final class EmptyCallRepository implements OcrNodeCallRepository {

        @Override
        public void save(OcrNodeCall call) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<OcrNodeCall> listByDocumentId(String documentId) {
            return List.of();
        }

        @Override
        public List<OcrNodeCall> listByBatchId(String batchId) {
            return List.of();
        }

        @Override
        public List<OcrNodeCall> listRecentByNodeId(String nodeId, int limit) {
            return List.of();
        }

        @Override
        public List<OcrNodeCall> listByNodeIdsAndDay(List<String> nodeIds, LocalDate day) {
            return List.of();
        }
    }

    /**
     * 固定节点仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static final class FixedNodeRepository implements io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository {

        private final List<OcrNode> nodes;

        private FixedNodeRepository(List<OcrNode> nodes) {
            this.nodes = nodes;
        }

        @Override
        public void save(OcrNode node) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveAll(List<OcrNode> nodes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(OcrNode node) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<OcrNode> findById(String nodeId) {
            return nodes.stream().filter(node -> node.id().equals(nodeId)).findFirst();
        }

        @Override
        public List<OcrNode> listByModelKey(String modelKey) {
            return nodes.stream().filter(node -> node.modelKey().equals(modelKey)).toList();
        }

        @Override
        public List<OcrNode> listEnabled() {
            return nodes.stream().filter(OcrNode::enabled).toList();
        }

        @Override
        public List<OcrNode> listAll() {
            return nodes;
        }

        @Override
        public Optional<OcrNode> findByModelHostPort(String modelKey, String host, int port) {
            return Optional.empty();
        }

        @Override
        public void deleteById(String nodeId) {
            throw new UnsupportedOperationException();
        }
    }
}
