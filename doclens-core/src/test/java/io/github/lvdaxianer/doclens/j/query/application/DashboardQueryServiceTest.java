package io.github.lvdaxianer.doclens.j.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.BASE_TIME;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.batch;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.completedDocument;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.emptyBatch;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.failedDocument;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.processingDocument;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.queuedBatch;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.stagedDocument;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardOcrMetricsTestFixtures.dashboardServiceWithOcrMetrics;

import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryBatchRepository;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryDocumentJobRepository;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryOcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        DashboardQueryService service = serviceWithDocuments(List.of(
                completedDocument("doc-completed", DocumentType.IMAGE, 0),
                failedDocument("doc-failed", DocumentType.PDF, 1),
                processingDocument("doc-processing", DocumentType.WORD, 2)
        ));

        Map<String, Object> summary = service.summary();

        assertThat(summary).containsKeys("overview", "recent_batches", "recent_failures", "recent_events");
        assertOverviewCounts(summary);
        assertRecentFailureCount(summary);
    }

    /**
     * 总览应返回细粒度阶段分布和 OCR 图片进度。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void summaryExposesStageBreakdownAndImageProgress() {
        DashboardQueryService service = serviceWithDocuments(List.of(
                stagedDocument("doc-word", DocumentType.WORD, ProcessingStage.WORD_TO_PDF_COMPLETED, 0, 1, 0),
                stagedDocument("doc-pdf", DocumentType.PDF, ProcessingStage.OCR_IMAGES, 3, 8, 1),
                stagedDocument("doc-save", DocumentType.IMAGE, ProcessingStage.SAVE_TEXT, 1, 1, 2),
                stagedDocument("doc-failed", DocumentType.WORD, ProcessingStage.WORD_TO_PDF, 0, 1, 3)
                        .fail("WORD_TO_PDF_FAILED", "convert failed", BASE_TIME.plusSeconds(8))
        ));

        Map<String, Object> summary = service.summary();

        assertStageBreakdown(summary);
        assertImageProgress(summary);
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

        assertOcrResources(summary);
    }

    /**
     * 最近批次应使用文档实时状态修正展示状态和进度。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void summaryShowsProcessingBatchWhenDocumentIsProcessing() {
        DashboardQueryService service = serviceWithQueuedBatchDocuments(List.of(
                stagedDocument("doc-processing", DocumentType.PDF, ProcessingStage.OCR_IMAGES, 541, 677, 0)
        ));

        Map<String, Object> summary = service.summary();

        assertProcessingBatchSummary(summary);
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
     * 创建默认批次下的 Dashboard 查询服务。
     *
     * @param documents 测试文档列表
     * @return Dashboard 查询服务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DashboardQueryService serviceWithDocuments(List<DocumentJob> documents) {
        return new DashboardQueryService(new InMemoryBatchRepository(List.of(batch())),
                new InMemoryDocumentJobRepository(documents), new InMemoryOcrEventRepository());
    }

    /**
     * 创建排队批次下的 Dashboard 查询服务。
     *
     * @param documents 测试文档列表
     * @return Dashboard 查询服务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DashboardQueryService serviceWithQueuedBatchDocuments(List<DocumentJob> documents) {
        return new DashboardQueryService(new InMemoryBatchRepository(List.of(queuedBatch())),
                new InMemoryDocumentJobRepository(documents), new InMemoryOcrEventRepository());
    }

    /**
     * 校验总览计数指标。
     *
     * @param summary 总览结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertOverviewCounts(Map<String, Object> summary) {
        assertThat(summary.get("overview")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("document_count", 3)
                .containsEntry("completed_documents", 1L)
                .containsEntry("failed_documents", 1L)
                .containsEntry("processing_documents", 1L)
                .containsEntry("success_rate", 33.33D)
                .containsEntry("failure_rate", 33.33D);
    }

    /**
     * 校验最近失败任务数量。
     *
     * @param summary 总览结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertRecentFailureCount(Map<String, Object> summary) {
        assertThat(summary.get("recent_failures")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .hasSize(1);
    }

    /**
     * 校验阶段分布指标。
     *
     * @param summary 总览结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertStageBreakdown(Map<String, Object> summary) {
        assertThat(summary.get("stage_status_counts")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .anySatisfy(row -> assertStageRow(row, "word_to_pdf_completed", 1L, Optional.of(0L)))
                .anySatisfy(row -> assertStageRow(row, "ocr_images", 3L, Optional.of(8L)))
                .anySatisfy(row -> assertStageRow(row, "failed", 1L, Optional.empty()));
    }

    /**
     * 校验单行阶段分布。
     *
     * @param row 阶段分布行
     * @param stage 阶段名称
     * @param firstValue 主要指标值
     * @param totalImages 总图片数
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertStageRow(Object row, String stage, long firstValue, Optional<Long> totalImages) {
        Map<?, ?> stageRow = (Map<?, ?>) row;
        assertThat(stageRow.get("stage")).isEqualTo(stage);
        assertStageValue(stageRow, stage, firstValue);
        totalImages.ifPresent(expectedTotalImages -> {
            // 只有带图片进度的阶段需要校验总图片数。
            assertThat(stageRow.get("total_images")).isEqualTo(expectedTotalImages);
        });
    }

    /**
     * 校验阶段主指标值。
     *
     * @param stageRow 阶段分布行
     * @param stage 阶段名称
     * @param firstValue 主要指标值
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertStageValue(Map<?, ?> stageRow, String stage, long firstValue) {
        if ("ocr_images".equals(stage)) {
            // OCR 阶段主指标是已完成图片数。
            assertThat(stageRow.get("completed_images")).isEqualTo(firstValue);
        } else {
            // 非 OCR 阶段主指标是文档数量。
            assertThat(stageRow.get("document_count")).isEqualTo(firstValue);
        }
    }

    /**
     * 校验图片总进度。
     *
     * @param summary 总览结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertImageProgress(Map<String, Object> summary) {
        assertThat(summary.get("image_progress")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("completed_images", 4L)
                .containsEntry("total_images", 9L);
    }

    /**
     * 校验 OCR 节点和线程池指标。
     *
     * @param summary 总览结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertOcrResources(Map<String, Object> summary) {
        assertThat(summary.get("ocr_resources")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("healthy_node_count", 2L)
                .containsEntry("down_node_count", 1L)
                .containsEntry("global_inflight_images", 7L);
        Map<?, ?> ocrResources = (Map<?, ?>) summary.get("ocr_resources");
        assertThat(ocrResources.get("thread_pools")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsKeys("ocr_request", "ocr_health");
    }

    /**
     * 校验最近批次的处理中状态。
     *
     * @param summary 总览结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertProcessingBatchSummary(Map<String, Object> summary) {
        assertThat(summary.get("recent_batches")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .singleElement()
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("status", "processing")
                .containsEntry("progress_percent", 77);
    }

}
