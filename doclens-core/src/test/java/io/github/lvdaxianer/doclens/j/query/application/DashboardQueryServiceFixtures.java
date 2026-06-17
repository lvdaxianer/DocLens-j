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

    /*
     * 本夹具只保留 Dashboard 查询测试的领域对象和内存仓储。
     * OCR 指标 provider 已拆到 DashboardOcrMetricsTestFixtures。
     * 这样总览、批次详情、OCR 路由测试可以共享基础数据，
     * 但不会把指标展示细节继续塞回通用夹具。
     * routedDocument 只描述文档上的路由策略元数据。
     * 真实命中节点由指标专用夹具负责提供。
     */

    static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-08T12:00:00+08:00");
    static final String TEST_CALLBACK_URL = "https://client.example.com/ocr-callback";
    static final String TEST_IDEMPOTENCY_KEY = "openwebui:file:file-123:hash:abc";
    static final String TEST_METADATA_FILE_ID = "file-123";
    static final String TEST_METADATA_SOURCE = "open-webui";
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
     * 创建带接入信息的测试批次。
     *
     * @return 带接入信息的测试批次
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    static Batch batchWithIntakeInfo() {
        Map<String, Object> metadata = Map.of("source", TEST_METADATA_SOURCE, "openwebui_file_id",
                TEST_METADATA_FILE_ID);
        return new Batch("batch-test", BatchStatus.COMPLETED, 1, 1, 0, Optional.empty(), Optional.empty(),
                "completed", new JsonPayload(metadata), Optional.of(TEST_CALLBACK_URL),
                Optional.of(TEST_IDEMPOTENCY_KEY), BASE_TIME, BASE_TIME.plusMinutes(5));
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
     * 创建无文档残留的空批次。
     *
     * @return 空批次
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    static Batch emptyBatch() {
        return new Batch("batch-empty", BatchStatus.COMPLETED, 0, 0, 0, Optional.empty(), Optional.empty(),
                "completed", JsonPayload.empty(), Optional.empty(), Optional.empty(), BASE_TIME,
                BASE_TIME.plusMinutes(30));
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
     * 内存批次仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    static class InMemoryBatchRepository implements BatchRepository {

        /*
         * 内存批次仓储只服务读模型测试。
         * 这里保留 save / updateSummary 是为了满足接口契约，
         * 实际测试只关注 findById 与 listRecent 的可预测返回。
         * listRecent 按更新时间倒序，贴近 Dashboard 最近批次展示。
         * updateSummary 为空实现，避免测试意外修改读模型输入。
         */

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

        /*
         * 文档仓储按 batchId 和 sortOrder 提供稳定结果。
         * Dashboard 详情页强依赖上传顺序，
         * 所以这里显式排序，避免 HashMap 迭代顺序影响断言。
         * saveAll / updateAll 仍委托单条方法，
         * 因为测试仓储没有外部 IO 或数据库往返成本。
         * 生产批量处理约束不在这个纯内存桩上触发。
         */

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

        /*
         * Dashboard 查询测试当前只读取 recent_events 的空集合。
         * 事件写入方法保留为空实现，
         * 是为了让服务构造保持真实接口形态。
         */

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
