package io.github.lvdaxianer.doclens.j.query.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
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

/**
 * Dashboard 查询服务测试夹具。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
final class DashboardQueryServiceFixtures {

    static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-08T12:00:00+08:00");
    private static final int TEST_BATCH_CAPACITY = 4;
    private static final int TEST_DOCUMENT_CAPACITY = 8;

    private DashboardQueryServiceFixtures() {
    }

    /**
     * 创建测试批次。
     *
     * @return 测试批次
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    static Batch batch() {
        return new Batch("batch-test", BatchStatus.COMPLETED, 3, 1, 1, Optional.empty(), Optional.empty(),
                "completed", JsonPayload.empty(), Optional.empty(), Optional.empty(), BASE_TIME,
                BASE_TIME.plusMinutes(5));
    }

    /**
     * 创建排队中的测试批次。
     *
     * @return 排队中的测试批次
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    static Batch queuedBatch() {
        return new Batch("batch-test", BatchStatus.QUEUED, 1, 0, 0, Optional.empty(), Optional.empty(),
                "queued", JsonPayload.empty(), Optional.empty(), Optional.empty(), BASE_TIME,
                BASE_TIME.plusMinutes(21));
    }

    /**
     * 创建已完成测试文档。
     *
     * @param documentId 文档 ID
     * @param fileType 文件类型
     * @param sortOrder 排序
     * @return 已完成测试文档
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    static DocumentJob completedDocument(String documentId, DocumentType fileType, int sortOrder) {
        return queuedDocument(documentId, fileType, sortOrder).complete("result-" + documentId,
                BASE_TIME.plusSeconds(10));
    }

    /**
     * 创建失败测试文档。
     *
     * @param documentId 文档 ID
     * @param fileType 文件类型
     * @param sortOrder 排序
     * @return 失败测试文档
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    static DocumentJob failedDocument(String documentId, DocumentType fileType, int sortOrder) {
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
     * @date 2026-06-09
     */
    static DocumentJob processingDocument(String documentId, DocumentType fileType, int sortOrder) {
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
     * @date 2026-06-09
     */
    static DocumentJob stagedDocument(
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
     * @date 2026-06-09
     */
    static DocumentJob queuedDocument(String documentId, DocumentType fileType, int sortOrder) {
        Optional<PdfMode> pdfMode = fileType == DocumentType.PDF
                ? Optional.of(PdfMode.PAGE_IMAGE_FALLBACK) : Optional.empty();
        DocumentJobCreateRequest request = new DocumentJobCreateRequest(documentId, "batch-test",
                documentId + ".dat", fileType, 10, 1, "local://" + documentId, "stub_ocr", pdfMode,
                JsonPayload.empty(), sortOrder, BASE_TIME);
        return DocumentJob.create(request);
    }

    /**
     * 创建带 OCR 路由策略的测试文档。
     *
     * @return 带路由策略的测试文档
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    static DocumentJob routedDocument() {
        DocumentJobCreateRequest request = new DocumentJobCreateRequest("doc-routed", "batch-test",
                "doc-routed.pdf", DocumentType.PDF, 10, 2, "local://doc-routed", "stub_ocr",
                Optional.of(PdfMode.PAGE_IMAGE_FALLBACK),
                OcrRoutePolicy.modelLoadBalance("paddle_ocr", "least-inflight"), JsonPayload.empty(), 0, BASE_TIME);
        return DocumentJob.create(request);
    }

    /**
     * 创建带测试 OCR 指标的 Dashboard 服务。
     *
     * @return Dashboard 查询服务
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    static DashboardQueryService dashboardServiceWithOcrMetrics() {
        return new DashboardQueryService(new InMemoryBatchRepository(List.of(batch())),
                new InMemoryDocumentJobRepository(List.of()), new InMemoryOcrEventRepository(),
                new TestDashboardOcrMetricsProvider());
    }

    /**
     * 内存批次仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    static class InMemoryBatchRepository implements BatchRepository {

        private final Map<String, Batch> batches = new HashMap<>(TEST_BATCH_CAPACITY);

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
     * @date 2026-06-09
     */
    static class InMemoryDocumentJobRepository implements DocumentJobRepository {

        private final Map<String, DocumentJob> documents = new HashMap<>(TEST_DOCUMENT_CAPACITY);

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
     * @date 2026-06-09
     */
    static class InMemoryOcrEventRepository implements OcrEventRepository {

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

    /**
     * 测试 OCR 指标提供器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    static class TestDashboardOcrMetricsProvider implements DashboardOcrMetricsProvider {

        @Override
        public Map<String, Object> ocrResources() {
            return Map.ofEntries(
                    Map.entry("healthy_node_count", 2L),
                    Map.entry("down_node_count", 1L),
                    Map.entry("recovering_node_count", 1L),
                    Map.entry("global_inflight_images", 7L),
                    Map.entry("busiest_node", Map.of("node_id", "node-1", "inflight_images", 5L)),
                    Map.entry("thread_pools", Map.of(
                            "ocr_request", Map.of("active_count", 2, "queue_size", 3),
                            "ocr_health", Map.of("active_count", 1, "queue_size", 0)))
            );
        }

        @Override
        public List<Map<String, Object>> dispatchHitNodesByBatch(String batchId) {
            return List.of(Map.of("model_key", "paddle_ocr", "node_id", "node-1", "image_count", 2L));
        }

        /**
         * 返回测试文档的最终分配节点。
         *
         * @param documentId 文档 ID
         * @return 最终分配节点列表
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        @Override
        public Map<String, List<Map<String, Object>>> finalHitNodesByBatch(String batchId) {
            return Map.of("doc-routed",
                    List.of(Map.of("model_key", "paddle_ocr", "node_id", "node-1", "image_count", 2L)));
        }
    }
}
