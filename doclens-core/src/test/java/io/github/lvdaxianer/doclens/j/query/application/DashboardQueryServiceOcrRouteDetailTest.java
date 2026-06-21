package io.github.lvdaxianer.doclens.j.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.batch;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.completedDocument;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.processingDocument;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.routedDocument;

import io.github.lvdaxianer.doclens.j.query.application.DashboardOcrMetricsTestFixtures.TestDashboardOcrMetricsProvider;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryBatchRepository;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryDocumentJobRepository;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryOcrEventRepository;
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
     * 批次详情应返回当前路由相关的 OCR 并发快照。
     *
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Test
    void batchDetailExposesOcrRuntimeCapacity() {
        DashboardQueryService service = new DashboardQueryService(new InMemoryBatchRepository(List.of(batch())),
                new InMemoryDocumentJobRepository(List.of(routedDocument())), new InMemoryOcrEventRepository(),
                new RuntimeCapacityDashboardOcrMetricsProvider());

        Map<String, Object> detail = service.batchDetail("batch-test");

        assertRuntimeCapacity(detail);
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
     * 批次详情应按文档返回 OCR 运行中分配节点，避免和最终分配混淆。
     *
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Test
    void batchDetailExposesRunningHitNodesScopedPerDocument() {
        DashboardQueryService service = new DashboardQueryService(new InMemoryBatchRepository(List.of(batch())),
                new InMemoryDocumentJobRepository(List.of(
                        processingDocument("doc-running", DocumentType.PDF, 0),
                        completedDocument("doc-done", DocumentType.PDF, 1)
                )), new InMemoryOcrEventRepository(), new RunningDashboardOcrMetricsProvider());

        List<?> documents = documentsOf(service.batchDetail("batch-test"));

        assertDocumentRunningHit(documents.get(0), "doc-running", "node-running");
        assertDocumentRunningHit(documents.get(1), "doc-done", "node-done");
    }

    /**
     * 批次详情应返回运行中的图片页任务，方便确认页码与线程级并发。
     *
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Test
    void batchDetailExposesRunningPageTasksWithThreadNames() {
        DashboardQueryService service = new DashboardQueryService(new InMemoryBatchRepository(List.of(batch())),
                new InMemoryDocumentJobRepository(List.of(processingDocument("doc-running", DocumentType.PDF, 0))),
                new InMemoryOcrEventRepository(), new RunningPageTaskDashboardOcrMetricsProvider());

        Map<String, Object> detail = service.batchDetail("batch-test");

        assertThat(detail.get("ocr_running_page_tasks"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .hasSize(2)
                .anySatisfy(row -> assertThat(row).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                        .containsEntry("document_id", "doc-running")
                        .containsEntry("page_no", 1)
                        .containsEntry("worker_id", "worker-a")
                        .containsEntry("thread_name", "doclens-page-task-ocr-1")
                        .containsEntry("model_key", "paddle_ocr")
                        .containsEntry("node_id", "node-1"))
                .anySatisfy(row -> assertThat(row).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                        .containsEntry("page_no", 2)
                        .containsEntry("thread_name", "doclens-page-task-ocr-2"));
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
     * 校验 OCR 运行态并发快照。
     *
     * @param detail 批次详情
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private void assertRuntimeCapacity(Map<String, Object> detail) {
        assertThat(detail.get("ocr_runtime_capacity"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("model_key", "paddle_ocr")
                .containsEntry("model_name", "PaddleOCR")
                .containsEntry("inflight_images", 6)
                .containsEntry("max_concurrency", 30);
        assertThat(detail.get("ocr_runtime_capacity"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .extractingByKey("nodes")
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .hasSize(2)
                .anySatisfy(row -> assertThat(row).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                        .containsEntry("node_id", "node-1")
                        .containsEntry("inflight_images", 2)
                        .containsEntry("max_concurrency", 10));
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
     * 校验指定文档的运行中命中节点。
     *
     * @param document 文档详情
     * @param documentId 文档 ID
     * @param nodeId OCR 节点 ID
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private void assertDocumentRunningHit(Object document, String documentId, String nodeId) {
        Map<?, ?> documentDetail = (Map<?, ?>) document;
        assertThat(documentDetail.get("document_id")).isEqualTo(documentId);
        assertThat(documentDetail.get("ocr_running_hit_nodes"))
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

        /**
         * 返回空文档运行中分配节点。
         *
         * @param batchId 批次 ID
         * @return 空运行中分配节点
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        @Override
        public Map<String, List<Map<String, Object>>> runningHitNodesByBatch(String batchId) {
            return Map.of();
        }

        @Override
        public Map<String, List<Map<String, Object>>> finalHitNodesByBatch(String batchId) {
            return Map.of(
                    "doc-1", List.of(Map.of("model_key", "paddle_ocr", "node_id", "node-1", "image_count", 2L)),
                    "doc-2", List.of(Map.of("model_key", "paddle_ocr", "node_id", "node-2", "image_count", 1L))
            );
        }
    }

    /**
     * 提供并发容量快照的测试指标提供器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private static class RuntimeCapacityDashboardOcrMetricsProvider extends TestDashboardOcrMetricsProvider {

        /**
         * 返回包含模型和节点并发容量的 OCR 资源快照。
         *
         * @return OCR 资源快照
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        @Override
        public Map<String, Object> ocrResources() {
            return Map.ofEntries(
                    Map.entry("models", List.of(Map.of(
                            "model_key", "paddle_ocr",
                            "model_name", "PaddleOCR",
                            "inflight_images", 6,
                            "max_concurrency", 30
                    ))),
                    Map.entry("nodes", List.of(
                            Map.of("model_key", "paddle_ocr", "node_id", "node-1", "node_name", "节点一",
                                    "inflight_images", 2, "max_concurrency", 10),
                            Map.of("model_key", "paddle_ocr", "node_id", "node-2", "node_name", "节点二",
                                    "inflight_images", 4, "max_concurrency", 20)
                    ))
            );
        }
    }

    /**
     * 提供文档级运行中分配节点的测试指标提供器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private static class RunningDashboardOcrMetricsProvider extends TestDashboardOcrMetricsProvider {

        /**
         * 返回文档级 OCR 运行中命中节点。
         *
         * @param batchId 批次 ID
         * @return 文档级运行中命中节点
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        @Override
        public Map<String, List<Map<String, Object>>> runningHitNodesByBatch(String batchId) {
            return Map.of(
                    "doc-running", List.of(Map.of("model_key", "paddle_ocr", "node_id", "node-running",
                            "image_count", 2L)),
                    "doc-done", List.of(Map.of("model_key", "paddle_ocr", "node_id", "node-done",
                            "image_count", 1L))
            );
        }
    }

    /**
     * 提供运行中图片页任务的测试指标提供器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private static class RunningPageTaskDashboardOcrMetricsProvider extends TestDashboardOcrMetricsProvider {

        /**
         * 返回运行中图片页任务。
         *
         * @param batchId 批次 ID
         * @return 运行中图片页任务
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        @Override
        public List<Map<String, Object>> runningPageTasksByBatch(String batchId) {
            return List.of(
                    Map.of("task_id", "task-1", "document_id", "doc-running", "page_no", 1,
                            "worker_id", "worker-a", "thread_name", "doclens-page-task-ocr-1",
                            "started_at", "2026-06-21T10:15:30+08:00", "running_ms", 1000L,
                            "model_key", "paddle_ocr", "node_id", "node-1"),
                    Map.of("task_id", "task-2", "document_id", "doc-running", "page_no", 2,
                            "worker_id", "worker-a", "thread_name", "doclens-page-task-ocr-2",
                            "started_at", "2026-06-21T10:15:31+08:00", "running_ms", 900L,
                            "model_key", "paddle_ocr", "node_id", "node-1")
            );
        }
    }
}
