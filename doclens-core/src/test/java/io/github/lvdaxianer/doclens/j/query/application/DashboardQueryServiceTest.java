package io.github.lvdaxianer.doclens.j.query.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.PdfMode;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
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

    private static final int TEST_BATCH_CAPACITY = 4;
    private static final int TEST_DOCUMENT_CAPACITY = 8;
    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-08T12:00:00+08:00");

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
        assertThat(activeNodes).containsExactly(true, true, true, true, true, true, true);
        List<String> wordStates = track.stream().map(node -> (String) ((Map<?, ?>) node).get("state")).toList();
        assertThat(wordStates).containsExactly("done", "done", "done", "done", "done", "done", "done");
        Map<?, ?> textDocument = (Map<?, ?>) documents.get(0);
        List<?> textTrack = (List<?>) textDocument.get("track");
        List<String> textStates = textTrack.stream().map(node -> (String) ((Map<?, ?>) node).get("state")).toList();
        assertThat(textStates).containsExactly("done", "done", "skipped", "skipped", "skipped", "skipped", "done");
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
        assertThat(states).containsExactly("done", "current", "skipped", "pending", "pending", "pending", "pending");
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
                "pending");
        Map<?, ?> failedDocument = (Map<?, ?>) documents.get(1);
        List<?> failedTrack = (List<?>) failedDocument.get("track");
        List<String> failedStates = failedTrack.stream().map(node -> (String) ((Map<?, ?>) node).get("state"))
                .toList();
        assertThat(failedStates).containsExactly("done", "done", "skipped", "done", "failed", "pending", "pending");
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
                "pending");
    }

    /**
     * 创建测试批次。
     *
     * @return 测试批次
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private Batch batch() {
        return new Batch("batch-test", BatchStatus.COMPLETED, 3, 1, 1, Optional.empty(), Optional.empty(),
                "completed", JsonPayload.empty(), Optional.empty(), Optional.empty(), BASE_TIME, BASE_TIME.plusMinutes(5));
    }

    /**
     * 创建排队中的测试批次。
     *
     * @return 排队中的测试批次
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private Batch queuedBatch() {
        return new Batch("batch-test", BatchStatus.QUEUED, 1, 0, 0, Optional.empty(), Optional.empty(),
                "queued", JsonPayload.empty(), Optional.empty(), Optional.empty(), BASE_TIME, BASE_TIME.plusMinutes(21));
    }

    /**
     * 创建已完成测试文档。
     *
     * @param documentId 文档 ID
     * @param fileType 文件类型
     * @param sortOrder 排序
     * @return 已完成测试文档
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private DocumentJob completedDocument(String documentId, DocumentType fileType, int sortOrder) {
        return queuedDocument(documentId, fileType, sortOrder).complete("result-" + documentId, BASE_TIME.plusSeconds(10));
    }

    /**
     * 创建失败测试文档。
     *
     * @param documentId 文档 ID
     * @param fileType 文件类型
     * @param sortOrder 排序
     * @return 失败测试文档
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private DocumentJob failedDocument(String documentId, DocumentType fileType, int sortOrder) {
        return queuedDocument(documentId, fileType, sortOrder)
                .fail("OCR_ERROR", "recognize failed", BASE_TIME.plusSeconds(20));
    }

    /**
     * 创建处理中测试文档。
     *
     * @param documentId 文档 ID
     * @param fileType 文件类型
     * @param sortOrder 排序
     * @return 处理中测试文档
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private DocumentJob processingDocument(String documentId, DocumentType fileType, int sortOrder) {
        return queuedDocument(documentId, fileType, sortOrder).startProcessing(BASE_TIME.plusSeconds(5));
    }

    /**
     * 创建指定阶段的测试文档。
     *
     * @param documentId 文档 ID
     * @param fileType 文件类型
     * @param stage 处理阶段
     * @param currentPage 当前图片页
     * @param totalPages 总图片页
     * @param sortOrder 排序
     * @return 指定阶段的测试文档
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private DocumentJob stagedDocument(
            String documentId,
            DocumentType fileType,
            ProcessingStage stage,
            int currentPage,
            int totalPages,
            int sortOrder
    ) {
        return queuedDocument(documentId, fileType, sortOrder)
                .startProcessing(BASE_TIME.plusSeconds(5))
                .advanceStage(stage, currentPage, totalPages, BASE_TIME.plusSeconds(6));
    }

    /**
     * 创建排队测试文档。
     *
     * @param documentId 文档 ID
     * @param fileType 文件类型
     * @param sortOrder 排序
     * @return 排队测试文档
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private DocumentJob queuedDocument(String documentId, DocumentType fileType, int sortOrder) {
        Optional<PdfMode> pdfMode = fileType == DocumentType.PDF
                ? Optional.of(PdfMode.PAGE_IMAGE_FALLBACK) : Optional.empty();
        DocumentJobCreateRequest request = new DocumentJobCreateRequest(documentId, "batch-test",
                documentId + ".dat", fileType, 10, 1, "local://" + documentId, "stub_ocr", pdfMode,
                JsonPayload.empty(), sortOrder, BASE_TIME);
        return DocumentJob.create(request);
    }

    /**
     * 内存批次仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class InMemoryBatchRepository implements BatchRepository {

        private final Map<String, Batch> batches = new HashMap<>(TEST_BATCH_CAPACITY);

        /**
         * 创建内存批次仓储。
         *
         * @param seedBatches 初始批次集合
         * @author lvdaxianerplus
         * @date 2026-06-08
         */
        InMemoryBatchRepository(List<Batch> seedBatches) {
            seedBatches.forEach(this::save);
        }

        @Override
        public void save(Batch batch) {
            batches.put(batch.batchId(), batch);
        }

        @Override
        public Optional<Batch> findById(String batchId) {
            return Optional.ofNullable(batches.get(batchId));
        }

        @Override
        public Optional<Batch> findByIdempotencyKey(String idempotencyKey) {
            return Optional.empty();
        }

        @Override
        public List<Batch> listRecent(int limit) {
            return batches.values().stream().sorted(Comparator.comparing(Batch::updatedAt).reversed())
                    .limit(limit).toList();
        }

        @Override
        public void updateSummary(String batchId, int completedFiles, int failedFiles, BatchStatus status) {
            // 当前测试只读取 Dashboard 读模型。
        }
    }

    /**
     * 内存文档仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class InMemoryDocumentJobRepository implements DocumentJobRepository {

        private final Map<String, DocumentJob> documents = new HashMap<>(TEST_DOCUMENT_CAPACITY);

        /**
         * 创建内存文档仓储。
         *
         * @param seedDocuments 初始文档集合
         * @author lvdaxianerplus
         * @date 2026-06-08
         */
        InMemoryDocumentJobRepository(List<DocumentJob> seedDocuments) {
            seedDocuments.forEach(this::save);
        }

        @Override
        public void save(DocumentJob document) {
            documents.put(document.documentId(), document);
        }

        @Override
        public void saveAll(List<DocumentJob> documents) {
            documents.forEach(this::save);
        }

        @Override
        public void update(DocumentJob document) {
            documents.put(document.documentId(), document);
        }

        @Override
        public void updateAll(List<DocumentJob> documents) {
            documents.forEach(this::update);
        }

        @Override
        public Optional<DocumentJob> findById(String documentId) {
            return Optional.ofNullable(documents.get(documentId));
        }

        @Override
        public List<DocumentJob> listByBatchId(String batchId) {
            return documents.values().stream().filter(document -> batchId.equals(document.batchId()))
                    .sorted(Comparator.comparing(DocumentJob::sortOrder)).toList();
        }

        @Override
        public List<DocumentJob> listByBatchIds(List<String> batchIds) {
            return documents.values().stream().filter(document -> batchIds.contains(document.batchId())).toList();
        }

        @Override
        public List<DocumentJob> listRecent(int limit) {
            return documents.values().stream().sorted(Comparator.comparing(DocumentJob::updatedAt).reversed())
                    .limit(limit).toList();
        }
    }

    /**
     * 空 OCR 事件仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class InMemoryOcrEventRepository implements OcrEventRepository {

        @Override
        public void save(OcrEvent event) {
            // 当前测试不写入 OCR 事件。
        }

        @Override
        public void saveAll(List<OcrEvent> events) {
            // 当前测试不写入 OCR 事件。
        }

        @Override
        public List<OcrEvent> listByBatchId(String batchId) {
            return List.of();
        }

        @Override
        public List<OcrEvent> listRecent(int limit) {
            return List.of();
        }
    }
}
