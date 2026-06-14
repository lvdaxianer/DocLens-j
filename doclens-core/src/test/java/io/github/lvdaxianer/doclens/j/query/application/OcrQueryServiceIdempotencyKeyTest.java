package io.github.lvdaxianer.doclens.j.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.PdfMode;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.shared.domain.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * OCR 查询服务的 idempotency key 回查测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-14
 */
class OcrQueryServiceIdempotencyKeyTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-14T12:00:00+08:00");

    /**
     * 验证批次可以通过 idempotency key 回查出批次快照和文档数组。
     *
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    @Test
    void getBatchByIdempotencyKeyReturnsReconciliationSnapshot() {
        Map<String, Object> queuedPayload = loadPayload(batch("batch-queued", BatchStatus.QUEUED, 1, 0, 0,
                "file-queued"), List.of(queuedDocument("doc-queued")));
        Map<String, Object> processingPayload = loadPayload(batch("batch-processing", BatchStatus.PROCESSING, 1, 0, 0,
                "file-processing"), List.of(processingDocument("doc-processing")));
        Map<String, Object> completedPayload = loadPayload(batch("batch-completed", BatchStatus.COMPLETED, 1, 1, 0,
                "file-completed"), List.of(completedDocument("doc-completed", "result-doc-completed")));
        Map<String, Object> failedPayload = loadPayload(batch("batch-failed", BatchStatus.FAILED, 1, 0, 1,
                "file-failed"), List.of(failedDocument("doc-failed", "OCR_FAILED", "OCR engine timeout")));

        assertBatchSnapshot(queuedPayload, "queued", "queued", "queued", "", "");
        assertBatchSnapshot(processingPayload, "processing", "processing", "ocr", "", "");
        assertBatchSnapshot(completedPayload, "completed", "completed", "completed", "result-doc-completed", "");
        assertBatchSnapshot(failedPayload, "failed", "failed", "failed", "", "OCR_FAILED");
    }

    /**
     * 验证找不到幂等键对应批次时会抛出资源不存在异常。
     *
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    @Test
    void getBatchByIdempotencyKeyThrowsWhenBatchMissing() {
        OcrQueryService service = new OcrQueryService(new FixedBatchRepository(null), new NoopDocumentJobRepository(),
                new NoopOcrResultRepository(), new NoopOcrEventRepository());

        assertThatThrownBy(() -> service.getBatchByIdempotencyKey("missing-file"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("batch not found for idempotency key missing-file");
    }

    /**
     * 创建测试文档。
     *
     * @param documentId 文档 ID
     * @param batchId 批次 ID
     * @param sortOrder 排序
     * @return 测试文档
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private DocumentJob document(String documentId, String batchId, int sortOrder) {
        DocumentJobCreateRequest request = new DocumentJobCreateRequest(documentId, batchId, documentId + ".pdf",
                DocumentType.PDF, 10L, 2, "local://uploads/" + documentId + ".pdf", "stub_ocr",
                Optional.of(PdfMode.PAGE_IMAGE_FALLBACK), OcrRoutePolicy.defaultPolicy(), JsonPayload.empty(),
                sortOrder, NOW);
        return DocumentJob.create(request).startProcessing(NOW).advanceStage(ProcessingStage.OCR_IMAGES, 1, 2, NOW);
    }

    /**
     * 创建待排队文档。
     *
     * @param documentId 文档 ID
     * @return 待排队文档
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private DocumentJob queuedDocument(String documentId) {
        return document(documentId, "batch-queued", 0).retry(NOW);
    }

    /**
     * 创建处理中文档。
     *
     * @param documentId 文档 ID
     * @return 处理中文档
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private DocumentJob processingDocument(String documentId) {
        return document(documentId, "batch-processing", 0);
    }

    /**
     * 创建已完成文档。
     *
     * @param documentId 文档 ID
     * @param resultId 结果 ID
     * @return 已完成文档
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private DocumentJob completedDocument(String documentId, String resultId) {
        return document(documentId, "batch-completed", 0).complete(resultId, NOW);
    }

    /**
     * 创建失败文档。
     *
     * @param documentId 文档 ID
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @return 失败文档
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private DocumentJob failedDocument(String documentId, String errorCode, String errorMessage) {
        return document(documentId, "batch-failed", 0).advanceStage(ProcessingStage.OCR_FAILED, 1, 2, NOW)
                .fail(errorCode, errorMessage, NOW);
    }

    /**
     * 构造批次快照。
     *
     * @param batchId 批次 ID
     * @param status 批次状态
     * @param totalFiles 总文件数
     * @param completedFiles 已完成文件数
     * @param failedFiles 失败文件数
     * @param idempotencyKey 幂等键
     * @return 批次
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private Batch batch(String batchId, BatchStatus status, int totalFiles, int completedFiles, int failedFiles,
            String idempotencyKey) {
        return new Batch(batchId, status, totalFiles, completedFiles, failedFiles, Optional.empty(), Optional.empty(),
                "ocr_images", JsonPayload.empty(), Optional.empty(), Optional.of(idempotencyKey), NOW, NOW);
    }

    /**
     * 加载测试快照。
     *
     * @param batch 批次
     * @param documents 文档列表
     * @return 回查快照
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private Map<String, Object> loadPayload(Batch batch, List<DocumentJob> documents) {
        OcrQueryService service = new OcrQueryService(new FixedBatchRepository(batch),
                new FixedDocumentJobRepository(documents), new NoopOcrResultRepository(), new NoopOcrEventRepository());
        return service.getBatchByIdempotencyKey(batch.idempotencyKey().orElseThrow());
    }

    /**
     * 断言批次与文档回查快照。
     *
     * @param payload 回查快照
     * @param batchStatus 批次状态
     * @param documentStatus 文档状态
     * @param stage 文档阶段
     * @param resultId 结果 ID
     * @param errorCode 错误码
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private void assertBatchSnapshot(Map<String, Object> payload, String batchStatus, String documentStatus, String stage,
            String resultId, String errorCode) {
        assertThat(payload).containsEntry("status", batchStatus);
        assertThat(payload.get("documents")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.list(Map.class))
                .hasSize(1);
        Map<String, Object> document = castDocument(payload);
        assertThat(document).containsEntry("status", documentStatus).containsEntry("stage", stage)
                .containsEntry("result_id", resultId);
        // 错误码为空时，说明当前快照没有业务错误。
        if (errorCode.isBlank()) {
            assertThat(castError(document)).containsEntry("code", "").containsEntry("message", "");
        } else {
            // 错误码非空时，必须返回对应的错误码。
            assertThat(castError(document)).containsEntry("code", errorCode);
        }
    }

    /**
     * 取出快照中的文档对象。
     *
     * @param payload 回查快照
     * @return 文档对象
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> castDocument(Map<String, Object> payload) {
        return (Map<String, Object>) ((List<?>) payload.get("documents")).get(0);
    }

    /**
     * 取出快照中的错误对象。
     *
     * @param document 文档对象
     * @return 错误对象
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> castError(Map<String, Object> document) {
        return (Map<String, Object>) document.get("error");
    }

    /**
     * 固定批次仓储。
     *
     * @param batch 测试批次
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private record FixedBatchRepository(Batch batch) implements BatchRepository {

        @Override
        public void save(Batch batch) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Batch> findById(String batchId) {
            // 批次 ID 匹配时返回测试批次。
            if (batch != null && batch.batchId().equals(batchId)) {
                return Optional.of(batch);
            } else {
                // 其他批次 ID 一律视为不存在。
                return Optional.empty();
            }
        }

        @Override
        public Optional<Batch> findByIdempotencyKey(String idempotencyKey) {
            // 幂等键匹配时返回测试批次。
            if (batch != null && batch.idempotencyKey().filter(idempotencyKey::equals).isPresent()) {
                return Optional.of(batch);
            } else {
                // 其他幂等键一律视为不存在。
                return Optional.empty();
            }
        }

        @Override
        public List<Batch> listRecent(int limit) {
            return List.of();
        }

        @Override
        public void updateSummary(String batchId, int completedFiles, int failedFiles, BatchStatus status) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * 固定文档仓储。
     *
     * @param documents 测试文档列表
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private record FixedDocumentJobRepository(List<DocumentJob> documents) implements DocumentJobRepository {

        @Override
        public void save(DocumentJob document) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveAll(List<DocumentJob> documents) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(DocumentJob document) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateAll(List<DocumentJob> documents) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<DocumentJob> findById(String documentId) {
            return documents.stream().filter(document -> document.documentId().equals(documentId)).findFirst();
        }

        @Override
        public List<DocumentJob> listByBatchId(String batchId) {
            return documents.stream().filter(document -> document.batchId().equals(batchId)).toList();
        }

        @Override
        public List<DocumentJob> listByBatchIds(List<String> batchIds) {
            return List.of();
        }

        @Override
        public List<DocumentJob> listRecent(int limit) {
            return List.of();
        }
    }

    /**
     * 空结果仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private static final class NoopOcrResultRepository implements io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository {

        @Override
        public void save(io.github.lvdaxianer.doclens.j.processing.domain.OcrResult result) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveAll(List<io.github.lvdaxianer.doclens.j.processing.domain.OcrResult> results) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<io.github.lvdaxianer.doclens.j.processing.domain.OcrResult> findByDocumentId(String documentId) {
            return Optional.empty();
        }
    }

    /**
     * 空事件仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private static final class NoopOcrEventRepository implements OcrEventRepository {

        @Override
        public void save(OcrEvent event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveAll(List<OcrEvent> events) {
            throw new UnsupportedOperationException();
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
     * 空文档仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private static final class NoopDocumentJobRepository implements DocumentJobRepository {

        @Override
        public void save(DocumentJob document) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveAll(List<DocumentJob> documents) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(DocumentJob document) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateAll(List<DocumentJob> documents) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<DocumentJob> findById(String documentId) {
            return Optional.empty();
        }

        @Override
        public List<DocumentJob> listByBatchId(String batchId) {
            return List.of();
        }

        @Override
        public List<DocumentJob> listByBatchIds(List<String> batchIds) {
            return List.of();
        }

        @Override
        public List<DocumentJob> listRecent(int limit) {
            return List.of();
        }
    }
}
