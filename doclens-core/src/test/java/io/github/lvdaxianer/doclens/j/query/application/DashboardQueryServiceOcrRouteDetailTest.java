package io.github.lvdaxianer.doclens.j.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.batch;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.completedDocument;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.routedDocument;

import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryBatchRepository;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryDocumentJobRepository;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryOcrEventRepository;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.TestDashboardOcrMetricsProvider;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Dashboard 批次详情 OCR 路由查询测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class DashboardQueryServiceOcrRouteDetailTest {

    /*
     * 本类只覆盖批次详情中的 OCR 路由读模型。
     * 普通处理轨道状态留在 DashboardQueryServiceBatchDetailTest。
     * 这样路由策略、批次命中节点、文档最终命中节点
     * 三类断言可以独立演进，不会把详情状态测试再次撑大。
     * 如果后续新增 OCR 路由展示字段，应优先放在这里。
     */

    /**
     * 批次详情应返回 OCR 路由策略，并拆分批次调度命中与文档最终分配节点。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void batchDetailExposesOcrRoutePolicyAndDispatchHits() {
        DashboardQueryService service = new DashboardQueryService(new InMemoryBatchRepository(List.of(batch())),
                new InMemoryDocumentJobRepository(List.of(routedDocument())), new InMemoryOcrEventRepository(),
                new TestDashboardOcrMetricsProvider());

        Map<String, Object> detail = service.batchDetail("batch-test");

        assertRoutePolicy(detail);
        assertDispatchHit(detail);
        assertFinalHit(detail);
    }

    /**
     * 批次详情中的文档最终分配节点不能串入其他文档的 OCR 命中数据。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void batchDetailKeepsFinalHitNodesScopedPerDocument() {
        DashboardQueryService service = new DashboardQueryService(new InMemoryBatchRepository(List.of(batch())),
                new InMemoryDocumentJobRepository(List.of(
                        completedDocument("doc-1", DocumentType.PDF, 0),
                        completedDocument("doc-2", DocumentType.PDF, 1)
                )), new InMemoryOcrEventRepository(), new ScopedDashboardOcrMetricsProvider());

        List<?> documents = documentsOf(service.batchDetail("batch-test"));

        assertDocumentFinalHit(documents.get(0), "doc-1", "node-1");
        assertDocumentFinalHit(documents.get(1), "doc-2", "node-2");
    }

    /**
     * 从详情结果中取出文档列表。
     *
     * @param detail 批次详情
     * @return 文档列表
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private List<?> documentsOf(Map<String, Object> detail) {
        return (List<?>) detail.get("documents");
    }

    /**
     * 校验 OCR 路由策略。
     *
     * @param detail 批次详情
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertRoutePolicy(Map<String, Object> detail) {
        assertThat(detail.get("ocr_route_policy")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("routing_mode", "MODEL_LOAD_BALANCE")
                .containsEntry("model_key", "paddle_ocr")
                .containsEntry("load_balance_strategy", "least-inflight");
    }

    /**
     * 校验批次调度命中节点。
     *
     * @param detail 批次详情
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertDispatchHit(Map<String, Object> detail) {
        assertThat(detail.get("batch_dispatch_hit_nodes")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .singleElement()
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("node_id", "node-1")
                .containsEntry("image_count", 2L);
    }

    /**
     * 校验文档最终命中节点。
     *
     * @param detail 批次详情
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertFinalHit(Map<String, Object> detail) {
        assertDocumentFinalHit(documentsOf(detail).getFirst(), "doc-routed", "node-1");
    }

    /**
     * 校验指定文档的最终命中节点。
     *
     * @param document 文档详情
     * @param documentId 文档 ID
     * @param nodeId OCR 节点 ID
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertDocumentFinalHit(Object document, String documentId, String nodeId) {
        Map<?, ?> documentDetail = (Map<?, ?>) document;
        assertThat(documentDetail.get("document_id")).isEqualTo(documentId);
        assertThat(documentDetail.get("ocr_final_hit_nodes"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .singleElement()
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("node_id", nodeId);
    }

    /**
     * 限定文档维度最终命中节点的测试指标提供器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static class ScopedDashboardOcrMetricsProvider implements DashboardOcrMetricsProvider {

        /*
         * 这个测试提供器刻意返回两个文档维度的最终命中节点。
         * 目标是验证 DashboardQueryService 不会把 batch 级命中数据
         * 误用到每个 document 的 ocr_final_hit_nodes 字段上。
         */

        @Override
        public Map<String, Object> ocrResources() {
            return Map.of();
        }

        @Override
        public List<Map<String, Object>> dispatchHitNodesByBatch(String batchId) {
            return List.of(
                    Map.of("model_key", "paddle_ocr", "node_id", "node-1", "image_count", 2L),
                    Map.of("model_key", "paddle_ocr", "node_id", "node-2", "image_count", 1L));
        }

        @Override
        public Map<String, List<Map<String, Object>>> finalHitNodesByBatch(String batchId) {
            return Map.of(
                    "doc-1", List.of(Map.of("model_key", "paddle_ocr", "node_id", "node-1", "image_count", 2L)),
                    "doc-2", List.of(Map.of("model_key", "paddle_ocr", "node_id", "node-2", "image_count", 1L))
            );
        }
    }
}
