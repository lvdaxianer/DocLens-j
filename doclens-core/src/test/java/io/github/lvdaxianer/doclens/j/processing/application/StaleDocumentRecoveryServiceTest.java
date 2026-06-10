package io.github.lvdaxianer.doclens.j.processing.application;

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
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 卡死文档恢复服务测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class StaleDocumentRecoveryServiceTest {

    private static final int TEST_CAPACITY = 8;
    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-10T14:00:00+08:00");

    /**
     * 超过 stale 阈值的 processing 文档应转为 stalled，并刷新批次摘要与事件。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void marksProcessingDocumentAsStalledWhenNoProgressBeyondThreshold() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository();
        InMemoryOcrEventRepository eventRepository = new InMemoryOcrEventRepository();
        DocumentJob stale = processingDocument("doc-stale", 0, BASE_TIME.minusMinutes(20));
        DocumentJob completed = completedDocument("doc-completed", 1, BASE_TIME.minusMinutes(5));
        documentRepository.saveAll(List.of(stale, completed));
        batchRepository.save(batch("batch-test", 2, 1, 0, BatchStatus.PROCESSING));
        StaleDocumentRecoveryService service = service(documentRepository, batchRepository, eventRepository);

        int stalledCount = service.markStaleDocuments(BASE_TIME);

        assertThat(stalledCount).isEqualTo(1);
        assertThat(documentRepository.findById("doc-stale")).get().satisfies(document -> {
            assertThat(document.status()).isEqualTo(DocumentStatus.STALLED);
            assertThat(document.stage()).isEqualTo(ProcessingStage.OCR_IMAGES);
            assertThat(document.errorCode()).contains("STALE_DOCUMENT");
            assertThat(document.errorMessage()).hasValueSatisfying(message -> assertThat(message).contains("stale"));
            assertThat(document.updatedAt()).isEqualTo(BASE_TIME);
        });
        assertThat(batchRepository.findById("batch-test")).get().satisfies(batch -> {
            assertThat(batch.completedFiles()).isEqualTo(1);
            assertThat(batch.failedFiles()).isEqualTo(1);
            assertThat(batch.status()).isEqualTo(BatchStatus.PARTIAL_FAILED);
        });
        assertThat(eventRepository.events)
                .extracting(OcrEvent::eventType)
                .contains(DocLensConstants.EVENT_DOCUMENT_STALLED);
    }

    /**
     * 尚未超过 stale 阈值的 processing 文档不应被误判为卡死。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void doesNotMarkRecentProcessingDocumentAsStalled() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository();
        InMemoryOcrEventRepository eventRepository = new InMemoryOcrEventRepository();
        documentRepository.save(processingDocument("doc-fresh", 0, BASE_TIME.minusSeconds(30)));
        batchRepository.save(batch("batch-test", 1, 0, 0, BatchStatus.PROCESSING));
        StaleDocumentRecoveryService service = service(documentRepository, batchRepository, eventRepository);

        int stalledCount = service.markStaleDocuments(BASE_TIME);

        assertThat(stalledCount).isZero();
        assertThat(documentRepository.findById("doc-fresh")).get().extracting(DocumentJob::status)
                .isEqualTo(DocumentStatus.PROCESSING);
        assertThat(eventRepository.events).isEmpty();
    }

    /**
     * 创建卡死恢复服务。
     *
     * @param documentRepository 文档仓储
     * @param batchRepository 批次仓储
     * @param eventRepository 事件仓储
     * @return 卡死恢复服务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private StaleDocumentRecoveryService service(
            InMemoryDocumentJobRepository documentRepository,
            InMemoryBatchRepository batchRepository,
            InMemoryOcrEventRepository eventRepository
    ) {
        return new StaleDocumentRecoveryService(
                new StaleDocumentRecoveryDependencies(documentRepository, batchRepository, eventRepository,
                        new OcrEventFactory(new IdGenerator())),
                new InlineTransactionRunner(),
                Duration.ofMinutes(5)
        );
    }

    /**
     * 创建 processing 文档。
     *
     * @param documentId 文档 ID
     * @param sortOrder 排序
     * @param updatedAt 更新时间
     * @return processing 文档
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentJob processingDocument(String documentId, int sortOrder, OffsetDateTime updatedAt) {
        DocumentJob created = DocumentJob.create(new DocumentJobCreateRequest(documentId, "batch-test",
                documentId + ".pdf", DocumentType.PDF, 10, 3, "local://" + documentId, "stub_ocr",
                Optional.empty(), JsonPayload.empty(), sortOrder, BASE_TIME.minusHours(1)));
        return new DocumentJob(created.documentId(), created.batchId(), created.fileName(), created.fileType(),
                created.fileSize(), created.pageCount(), created.storageUri(), DocumentStatus.PROCESSING,
                ProcessingStage.OCR_IMAGES, 55, 2, 3, created.adapterName(), created.pdfMode(),
                created.ocrRoutePolicy(), created.metadata(), created.resultId(), created.errorCode(),
                created.errorMessage(), created.sortOrder(), created.createdAt(), updatedAt);
    }

    /**
     * 创建已完成文档。
     *
     * @param documentId 文档 ID
     * @param sortOrder 排序
     * @param updatedAt 更新时间
     * @return 已完成文档
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentJob completedDocument(String documentId, int sortOrder, OffsetDateTime updatedAt) {
        DocumentJob created = DocumentJob.create(new DocumentJobCreateRequest(documentId, "batch-test",
                documentId + ".pdf", DocumentType.PDF, 10, 3, "local://" + documentId, "stub_ocr",
                Optional.empty(), JsonPayload.empty(), sortOrder, BASE_TIME.minusHours(1)));
        return new DocumentJob(created.documentId(), created.batchId(), created.fileName(), created.fileType(),
                created.fileSize(), created.pageCount(), created.storageUri(), DocumentStatus.COMPLETED,
                ProcessingStage.COMPLETED, 100, 3, 3, created.adapterName(), created.pdfMode(),
                created.ocrRoutePolicy(), created.metadata(), Optional.of("result-" + documentId),
                Optional.empty(), Optional.empty(), created.sortOrder(), created.createdAt(), updatedAt);
    }

    /**
     * 创建测试批次。
     *
     * @param batchId 批次 ID
     * @param totalFiles 总文件数
     * @param completedFiles 已完成文件数
     * @param failedFiles 失败文件数
     * @param status 批次状态
     * @return 批次
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private Batch batch(String batchId, int totalFiles, int completedFiles, int failedFiles, BatchStatus status) {
        return new Batch(batchId, status, totalFiles, completedFiles, failedFiles, Optional.empty(),
                Optional.empty(), status.name().toLowerCase(), JsonPayload.empty(), Optional.empty(),
                Optional.empty(), BASE_TIME.minusHours(1), BASE_TIME.minusMinutes(1));
    }

    /**
     * 内存文档仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class InMemoryDocumentJobRepository implements DocumentJobRepository {

        private final Map<String, DocumentJob> documents = new HashMap<>(TEST_CAPACITY);

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
            return documents.values().stream()
                    .filter(document -> batchId.equals(document.batchId()))
                    .sorted((left, right) -> Integer.compare(left.sortOrder(), right.sortOrder()))
                    .toList();
        }

        @Override
        public List<DocumentJob> listByBatchIds(List<String> batchIds) {
            return documents.values().stream().filter(document -> batchIds.contains(document.batchId())).toList();
        }

        @Override
        public List<DocumentJob> listRecent(int limit) {
            return documents.values().stream()
                    .sorted((left, right) -> right.updatedAt().compareTo(left.updatedAt()))
                    .limit(limit)
                    .toList();
        }
    }

    /**
     * 内存批次仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class InMemoryBatchRepository implements BatchRepository {

        private final Map<String, Batch> batches = new HashMap<>(TEST_CAPACITY);

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
            return batches.values().stream().limit(limit).toList();
        }

        @Override
        public void updateSummary(String batchId, int completedFiles, int failedFiles, BatchStatus status) {
            Batch batch = batches.get(batchId);
            if (batch != null) {
                batches.put(batchId, new Batch(batch.batchId(), status, batch.totalFiles(), completedFiles,
                        failedFiles, batch.currentDocumentId(), batch.currentDocumentName(),
                        status.name().toLowerCase(), batch.metadata(), batch.callbackUrl(),
                        batch.idempotencyKey(), batch.createdAt(), BASE_TIME));
            }
        }
    }

    /**
     * 内存事件仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class InMemoryOcrEventRepository implements OcrEventRepository {

        private final List<OcrEvent> events = new ArrayList<>(TEST_CAPACITY);

        @Override
        public void save(OcrEvent event) {
            events.add(event);
        }

        @Override
        public void saveAll(List<OcrEvent> events) {
            this.events.addAll(events);
        }

        @Override
        public List<OcrEvent> listByBatchId(String batchId) {
            return events.stream().filter(event -> batchId.equals(event.batchId())).toList();
        }

        @Override
        public List<OcrEvent> listRecent(int limit) {
            return events.stream().limit(limit).toList();
        }
    }

    /**
     * 直接执行事务的测试事务器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class InlineTransactionRunner implements TransactionRunner {

        @Override
        public void requiredVoid(Runnable action) {
            action.run();
        }

        @Override
        public <T> T requiredResult(java.util.function.Supplier<T> action) {
            return action.get();
        }
    }
}
