package io.github.lvdaxianer.doclens.j.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.BASE_TIME;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.batch;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.completedDocument;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.emptyBatch;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.dashboardServiceWithOcrMetrics;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.failedDocument;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.processingDocument;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.queuedBatch;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.queuedDocument;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.routedDocument;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.stagedDocument;

import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryBatchRepository;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryDocumentJobRepository;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryOcrEventRepository;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.TestDashboardOcrMetricsProvider;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Dashboard 读模型查询服务测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class DashboardQueryServiceTest {

    /**
     * 总览应聚合批次、文档状态和最近失败任务。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void summaryAggregatesDashboardOverview() {
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository(List.of(batch()));
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository(List.of(
                completedDocument("doc-completed", DocumentType.IMAGE, 0),
                failedDocument("doc-failed", DocumentType.PDF, 1),
                processingDocument("doc-processing", DocumentType.WORD, 2)
        ));
        DashboardQueryService service = new DashboardQueryService(batchRepository, documentRepository,
                new InMemoryOcrEventRepository());

        Map<String, Object> summary = service.summary();

        assertThat(summary).containsKeys("overview", "recent_batches", "recent_failures", "recent_events");
        assertThat(summary.get("overview")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("document_count", 3)
                .containsEntry("completed_documents", 1L)
                .containsEntry("failed_documents", 1L)
                .containsEntry("processing_documents", 1L)
                .containsEntry("success_rate", 33.33D)
                .containsEntry("failure_rate", 33.33D);
        assertThat(summary.get("recent_failures")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .hasSize(1);
    }

    /**
     * 总览应返回细粒度阶段分布和 OCR 图片进度。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void summaryExposesStageBreakdownAndImageProgress() {
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository(List.of(batch()));
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository(List.of(
                stagedDocument("doc-word", DocumentType.WORD, ProcessingStage.WORD_TO_PDF_COMPLETED, 0, 1, 0),
                stagedDocument("doc-pdf", DocumentType.PDF, ProcessingStage.OCR_IMAGES, 3, 8, 1),
                stagedDocument("doc-save", DocumentType.IMAGE, ProcessingStage.SAVE_TEXT, 1, 1, 2),
                stagedDocument("doc-failed", DocumentType.WORD, ProcessingStage.WORD_TO_PDF, 0, 1, 3)
                        .fail("WORD_TO_PDF_FAILED", "convert failed", BASE_TIME.plusSeconds(8))
        ));
        DashboardQueryService service = new DashboardQueryService(batchRepository, documentRepository,
                new InMemoryOcrEventRepository());

        Map<String, Object> summary = service.summary();

        assertThat(summary.get("stage_status_counts")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .anySatisfy(row -> assertThat(row).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                        .containsEntry("stage", "word_to_pdf_completed")
                        .containsEntry("document_count", 1L)
                        .containsEntry("total_images", 0L))
                .anySatisfy(row -> assertThat(row).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                        .containsEntry("stage", "ocr_images")
                        .containsEntry("completed_images", 3L)
                        .containsEntry("total_images", 8L))
                .anySatisfy(row -> assertThat(row).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                        .containsEntry("stage", "failed")
                        .containsEntry("document_count", 1L));
        assertThat(summary.get("image_progress")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("completed_images", 4L)
                .containsEntry("total_images", 9L);
    }

    /**
     * 总览应返回 OCR 节点和线程池指标。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void summaryExposesOcrResourceAndThreadPoolMetrics() {
        DashboardQueryService service = dashboardServiceWithOcrMetrics();

        Map<String, Object> summary = service.summary();

        assertThat(summary.get("ocr_resources")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("healthy_node_count", 2L)
                .containsEntry("down_node_count", 1L)
                .containsEntry("global_inflight_images", 7L);
        Map<?, ?> ocrResources = (Map<?, ?>) summary.get("ocr_resources");
        assertThat(ocrResources.get("thread_pools")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsKeys("ocr_request", "ocr_health");
    }

    /**
     * 最近批次应使用文档实时状态修正展示状态和进度。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void summaryShowsProcessingBatchWhenDocumentIsProcessing() {
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository(List.of(queuedBatch()));
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository(List.of(
                stagedDocument("doc-processing", DocumentType.PDF, ProcessingStage.OCR_IMAGES, 541, 677, 0)
        ));
        DashboardQueryService service = new DashboardQueryService(batchRepository, documentRepository,
                new InMemoryOcrEventRepository());

        Map<String, Object> summary = service.summary();

        assertThat(summary.get("recent_batches")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .singleElement()
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("status", "processing")
                .containsEntry("progress_percent", 77);
    }

    /**
     * 总览不应继续展示已无文档的空批次。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void summaryDoesNotExposeBatchWithoutDocuments() {
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository(List.of(emptyBatch()));
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository(List.of());
        DashboardQueryService service = new DashboardQueryService(batchRepository, documentRepository,
                new InMemoryOcrEventRepository());

        Map<String, Object> summary = service.summary();

        assertThat(summary.get("recent_batches")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .isEmpty();
    }

    /**
     * 批次详情应按上传顺序返回文档，并包含文件类型对应的处理轨道。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void batchDetailReturnsOrderedDocumentsAndProcessingTrack() {
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository(List.of(batch()));
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository(List.of(
                completedDocument("doc-word", DocumentType.WORD, 1),
                completedDocument("doc-text", DocumentType.TEXT, 0)
        ));
        DashboardQueryService service = new DashboardQueryService(batchRepository, documentRepository,
                new InMemoryOcrEventRepository());

        Map<String, Object> detail = service.batchDetail("batch-test");

        List<?> documents = (List<?>) detail.get("documents");
        List<String> documentIds = documents.stream().map(document -> (String) ((Map<?, ?>) document).get("document_id"))
                .toList();
        assertThat(documentIds).containsExactly("doc-text", "doc-word");
        Map<?, ?> wordDocument = (Map<?, ?>) documents.get(1);
        List<?> track = (List<?>) wordDocument.get("track");
        List<Boolean> activeNodes = track.stream().map(node -> (Boolean) ((Map<?, ?>) node).get("active")).toList();
        assertThat(activeNodes).containsExactly(true, true, true, true, true, true, true, true);
        List<String> wordStates = track.stream().map(node -> (String) ((Map<?, ?>) node).get("state")).toList();
        assertThat(wordStates).containsExactly("done", "done", "done", "done", "done", "done", "done", "done");
        Map<?, ?> textDocument = (Map<?, ?>) documents.get(0);
        List<?> textTrack = (List<?>) textDocument.get("track");
        List<String> textStates = textTrack.stream().map(node -> (String) ((Map<?, ?>) node).get("state")).toList();
        assertThat(textStates).containsExactly("done", "done", "skipped", "skipped", "skipped", "skipped", "skipped",
                "done");
    }

    /**
     * 批次详情应返回 OCR 路由策略，并拆分批次调度命中与文档最终分配节点。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void batchDetailExposesOcrRoutePolicyAndDispatchHits() {
        DocumentJob document = routedDocument();
        DashboardQueryService service = new DashboardQueryService(new InMemoryBatchRepository(List.of(batch())),
                new InMemoryDocumentJobRepository(List.of(document)), new InMemoryOcrEventRepository(),
                new TestDashboardOcrMetricsProvider());

        Map<String, Object> detail = service.batchDetail("batch-test");

        assertThat(detail.get("ocr_route_policy")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("routing_mode", "MODEL_LOAD_BALANCE")
                .containsEntry("model_key", "paddle_ocr")
                .containsEntry("load_balance_strategy", "least-inflight");
        assertThat(detail.get("batch_dispatch_hit_nodes")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .singleElement()
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("node_id", "node-1")
                .containsEntry("image_count", 2L);
        assertThat(detail.get("documents")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .singleElement()
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .extractingByKey("ocr_final_hit_nodes")
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .singleElement()
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("node_id", "node-1")
                .containsEntry("image_count", 2L);
    }

    /**
     * 批次详情中的文档最终分配节点不能串入其他文档的 OCR 命中数据。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void batchDetailKeepsFinalHitNodesScopedPerDocument() {
        DocumentJob firstDocument = completedDocument("doc-1", DocumentType.PDF, 0);
        DocumentJob secondDocument = completedDocument("doc-2", DocumentType.PDF, 1);
        DashboardQueryService service = new DashboardQueryService(new InMemoryBatchRepository(List.of(batch())),
                new InMemoryDocumentJobRepository(List.of(firstDocument, secondDocument)),
                new InMemoryOcrEventRepository(), new DashboardOcrMetricsProvider() {
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
                                "doc-1",
                                List.of(Map.of("model_key", "paddle_ocr", "node_id", "node-1", "image_count", 2L)),
                                "doc-2",
                                List.of(Map.of("model_key", "paddle_ocr", "node_id", "node-2", "image_count", 1L))
                        );
                    }
                });

        Map<String, Object> detail = service.batchDetail("batch-test");

        assertThat(detail.get("documents")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .satisfies(documents -> {
                    Map<?, ?> first = (Map<?, ?>) documents.get(0);
                    Map<?, ?> second = (Map<?, ?>) documents.get(1);
                    assertThat(first.get("document_id")).isEqualTo("doc-1");
                    assertThat(first.get("ocr_final_hit_nodes"))
                            .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                            .singleElement()
                            .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                            .containsEntry("node_id", "node-1")
                            .doesNotContainEntry("node_id", "node-2");
                    assertThat(second.get("document_id")).isEqualTo("doc-2");
                    assertThat(second.get("ocr_final_hit_nodes"))
                            .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                            .singleElement()
                            .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                            .containsEntry("node_id", "node-2")
                            .doesNotContainEntry("node_id", "node-1");
                });
    }

    /**
     * 批次详情应展示待解析文档的当前步骤和未执行步骤。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void batchDetailShowsQueuedDocumentTrackStates() {
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository(List.of(batch()));
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository(List.of(
                queuedDocument("doc-pdf", DocumentType.PDF, 0)
        ));
        DashboardQueryService service = new DashboardQueryService(batchRepository, documentRepository,
                new InMemoryOcrEventRepository());

        Map<String, Object> detail = service.batchDetail("batch-test");

        List<?> documents = (List<?>) detail.get("documents");
        Map<?, ?> document = (Map<?, ?>) documents.getFirst();
        List<?> track = (List<?>) document.get("track");
        List<String> states = track.stream().map(node -> (String) ((Map<?, ?>) node).get("state")).toList();
        assertThat(states).containsExactly("done", "current", "skipped", "pending", "pending", "pending", "pending",
                "pending");
    }

    /**
     * 批次详情应展示 OCR 处理中和 OCR 失败节点。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void batchDetailShowsOcrProcessingAndFailedTrackStates() {
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository(List.of(batch()));
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository(List.of(
                stagedDocument("doc-processing", DocumentType.PDF, ProcessingStage.OCR_IMAGES, 2, 5, 0),
                stagedDocument("doc-failed", DocumentType.PDF, ProcessingStage.OCR_IMAGES, 2, 5, 1)
                        .fail("OCR_FAILED", "ocr failed", BASE_TIME.plusSeconds(7))
        ));
        DashboardQueryService service = new DashboardQueryService(batchRepository, documentRepository,
                new InMemoryOcrEventRepository());

        Map<String, Object> detail = service.batchDetail("batch-test");

        List<?> documents = (List<?>) detail.get("documents");
        Map<?, ?> processingDocument = (Map<?, ?>) documents.get(0);
        List<?> processingTrack = (List<?>) processingDocument.get("track");
        List<String> processingStates = processingTrack.stream()
                .map(node -> (String) ((Map<?, ?>) node).get("state")).toList();
        assertThat(processingStates).containsExactly("done", "done", "skipped", "done", "current", "pending",
                "pending", "pending");
        Map<?, ?> failedDocument = (Map<?, ?>) documents.get(1);
        List<?> failedTrack = (List<?>) failedDocument.get("track");
        List<String> failedStates = failedTrack.stream().map(node -> (String) ((Map<?, ?>) node).get("state"))
                .toList();
        assertThat(failedStates).containsExactly("done", "done", "skipped", "done", "failed", "pending", "pending",
                "pending");
    }

    /**
     * 批次详情应按失败前阶段展示转换失败节点。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void batchDetailShowsConversionFailedTrackState() {
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository(List.of(batch()));
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository(List.of(
                stagedDocument("doc-failed", DocumentType.WORD, ProcessingStage.WORD_TO_PDF, 0, 1, 0)
                        .fail("WORD_TO_PDF_FAILED", "convert failed", BASE_TIME.plusSeconds(7))
        ));
        DashboardQueryService service = new DashboardQueryService(batchRepository, documentRepository,
                new InMemoryOcrEventRepository());

        Map<String, Object> detail = service.batchDetail("batch-test");

        List<?> documents = (List<?>) detail.get("documents");
        Map<?, ?> failedDocument = (Map<?, ?>) documents.getFirst();
        List<?> failedTrack = (List<?>) failedDocument.get("track");
        List<String> failedStates = failedTrack.stream().map(node -> (String) ((Map<?, ?>) node).get("state"))
                .toList();
        assertThat(failedStates).containsExactly("done", "done", "failed", "pending", "pending", "pending",
                "pending", "pending");
    }

    /**
     * 批次详情应展示独立的 LLM Markdown 后处理步骤。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void batchDetailShowsLlmMarkdownStepBetweenMergeAndSave() {
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository(List.of(batch()));
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository(List.of(
                stagedDocument("doc-word", DocumentType.WORD, ProcessingStage.SAVE_TEXT, 4, 4, 0)
        ));
        DashboardQueryService service = new DashboardQueryService(batchRepository, documentRepository,
                new InMemoryOcrEventRepository());

        Map<String, Object> detail = service.batchDetail("batch-test");

        List<?> documents = (List<?>) detail.get("documents");
        Map<?, ?> document = (Map<?, ?>) documents.getFirst();
        List<?> track = (List<?>) document.get("track");
        List<String> labels = track.stream().map(node -> (String) ((Map<?, ?>) node).get("name")).toList();
        List<String> states = track.stream().map(node -> (String) ((Map<?, ?>) node).get("state")).toList();
        assertThat(labels).containsExactly("上传", "类型识别", "转换", "渲染页图", "OCR", "合并文本", "LLM 排版", "入库/落盘");
        assertThat(states).containsExactly("done", "done", "done", "done", "done", "done", "current", "pending");
    }

}
