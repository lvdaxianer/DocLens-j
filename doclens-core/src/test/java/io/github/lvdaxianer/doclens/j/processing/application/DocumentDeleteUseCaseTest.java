package io.github.lvdaxianer.doclens.j.processing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.PdfMode;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 文档删除用例测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class DocumentDeleteUseCaseTest {

    private static final int TEST_CAPACITY = 8;

    /**
     * 失败文档删除后应清理文档、结果、事件和关联存储，并刷新批次摘要。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void deleteFailedDocumentCleansDependenciesAndRefreshesBatchSummary() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        InMemoryOcrEventRepository eventRepository = new InMemoryOcrEventRepository();
        RecordingObjectStorage objectStorage = new RecordingObjectStorage();
        OffsetDateTime now = OffsetDateTime.now();
        DocumentJob failedDocument = document("doc-failed", 0)
                .startProcessing(now)
                .fail(DocLensConstants.ERROR_CODE_OCR_FAILED, "ocr failed", now.plusSeconds(1));
        DocumentJob completedDocument = document("doc-completed", 1)
                .startProcessing(now)
                .complete("result-doc-completed", now.plusSeconds(2));
        documentRepository.saveAll(List.of(failedDocument, completedDocument));
        batchRepository.save(batch(2, 1, 1, BatchStatus.PARTIAL_FAILED));
        resultRepository.save(result("doc-failed", "local://results/doc-failed.md"));
        eventRepository.saveAll(List.of(documentEvent("doc-failed", DocLensConstants.EVENT_DOCUMENT_FAILED),
                documentEvent("doc-completed", DocLensConstants.EVENT_DOCUMENT_COMPLETED)));
        DocumentDeleteUseCase useCase = useCase(documentRepository, batchRepository, resultRepository,
                eventRepository, objectStorage);

        useCase.delete("doc-failed");

        assertThat(documentRepository.findById("doc-failed")).isEmpty();
        assertThat(resultRepository.findByDocumentId("doc-failed")).isEmpty();
        assertThat(eventRepository.listByBatchId("batch-test"))
                .extracting(OcrEvent::documentId)
                .containsExactly(Optional.of("doc-completed"), Optional.of("doc-failed"));
        assertThat(objectStorage.deletedUris).containsExactly("local://doc-failed", "local://results/doc-failed.md");
        assertThat(batchRepository.findById("batch-test")).get().satisfies(batch -> {
            assertThat(batch.completedFiles()).isEqualTo(1);
            assertThat(batch.failedFiles()).isZero();
            assertThat(batch.status()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(batch.totalFiles()).isEqualTo(1);
        });
    }

    /**
     * 已完成和卡死文档也应允许删除，便于用户清理历史结果或恢复失败残留。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void deleteCompletedAndStalledDocumentsIsAllowed() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        InMemoryOcrEventRepository eventRepository = new InMemoryOcrEventRepository();
        RecordingObjectStorage objectStorage = new RecordingObjectStorage();
        OffsetDateTime now = OffsetDateTime.now();
        DocumentJob completedDocument = document("doc-completed", 0)
                .startProcessing(now)
                .complete("result-doc-completed", now.plusSeconds(1));
        DocumentJob stalledDocument = new DocumentJob("doc-stalled", "batch-test", "doc-stalled.txt",
                DocumentType.TEXT, 5, 1, "local://doc-stalled", DocumentStatus.STALLED, ProcessingStage.OCR_IMAGES,
                50, 1, 1, "stub_ocr", Optional.<PdfMode>empty(),
                io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy.defaultPolicy(), JsonPayload.empty(),
                Optional.empty(), Optional.of("STALE_DOCUMENT"), Optional.of("stalled"), 1, now, now.plusSeconds(10));
        documentRepository.saveAll(List.of(completedDocument, stalledDocument));
        batchRepository.save(batch(2, 1, 0, BatchStatus.PROCESSING));
        resultRepository.save(result("doc-completed", "local://results/doc-completed.md"));
        DocumentDeleteUseCase useCase = useCase(documentRepository, batchRepository, resultRepository,
                eventRepository, objectStorage);

        useCase.delete("doc-completed");
        useCase.delete("doc-stalled");

        assertThat(documentRepository.listByBatchId("batch-test")).isEmpty();
        assertThat(resultRepository.findByDocumentId("doc-completed")).isEmpty();
        assertThat(objectStorage.deletedUris)
                .contains("local://results/doc-completed.md", "local://doc-completed", "local://doc-stalled");
    }

    /**
     * 删除批次内最后一个文档后，不应保留空批次残留。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void deleteLastDocumentRemovesEmptyBatch() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        InMemoryOcrEventRepository eventRepository = new InMemoryOcrEventRepository();
        RecordingObjectStorage objectStorage = new RecordingObjectStorage();
        OffsetDateTime now = OffsetDateTime.now();
        DocumentJob completedDocument = document("doc-last", 0)
                .startProcessing(now)
                .complete("result-doc-last", now.plusSeconds(1));
        documentRepository.save(completedDocument);
        batchRepository.save(batch(1, 1, 0, BatchStatus.COMPLETED));
        resultRepository.save(result("doc-last", "local://results/doc-last.md"));
        DocumentDeleteUseCase useCase = useCase(documentRepository, batchRepository, resultRepository,
                eventRepository, objectStorage);

        useCase.delete("doc-last");

        assertThat(documentRepository.findById("doc-last")).isEmpty();
        assertThat(batchRepository.findById("batch-test")).isEmpty();
    }

    /**
     * 处理中删除在第一版应明确拒绝，避免破坏运行中的处理链路。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void deleteProcessingDocumentIsRejected() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        InMemoryOcrEventRepository eventRepository = new InMemoryOcrEventRepository();
        RecordingObjectStorage objectStorage = new RecordingObjectStorage();
        OffsetDateTime now = OffsetDateTime.now();
        DocumentJob processingDocument = document("doc-processing", 0).startProcessing(now);
        documentRepository.save(processingDocument);
        batchRepository.save(batch(1, 0, 0, BatchStatus.PROCESSING));
        DocumentDeleteUseCase useCase = useCase(documentRepository, batchRepository, resultRepository,
                eventRepository, objectStorage);

        assertThatThrownBy(() -> useCase.delete("doc-processing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("processing");
        assertThat(documentRepository.findById("doc-processing")).isPresent();
        assertThat(objectStorage.deletedUris).isEmpty();
    }

    /**
     * 创建待测文档删除用例。
     *
     * @param documentRepository 文档仓储
     * @param batchRepository 批次仓储
     * @param resultRepository 结果仓储
     * @param eventRepository 事件仓储
     * @param objectStorage 对象存储
     * @return 文档删除用例
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentDeleteUseCase useCase(
            InMemoryDocumentJobRepository documentRepository,
            InMemoryBatchRepository batchRepository,
            InMemoryOcrResultRepository resultRepository,
            InMemoryOcrEventRepository eventRepository,
            RecordingObjectStorage objectStorage
    ) {
        return new DocumentDeleteUseCase(new DocumentDeleteDependencies(documentRepository, batchRepository,
                resultRepository, eventRepository, objectStorage, new OcrEventFactory(new IdGenerator())),
                new InlineTransactionRunner());
    }

    /**
     * 创建测试文档。
     *
     * @param documentId 文档 ID
     * @param sortOrder 排序
     * @return 测试文档
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentJob document(String documentId, int sortOrder) {
        return DocumentJob.create(new DocumentJobCreateRequest(documentId, "batch-test", documentId + ".txt",
                DocumentType.TEXT, 5, 1, "local://" + documentId, "stub_ocr", Optional.empty(),
                JsonPayload.empty(), sortOrder, OffsetDateTime.now()));
    }

    /**
     * 创建测试批次。
     *
     * @param totalFiles 总文件数
     * @param completedFiles 已完成文件数
     * @param failedFiles 失败文件数
     * @param status 批次状态
     * @return 测试批次
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private Batch batch(int totalFiles, int completedFiles, int failedFiles, BatchStatus status) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Batch("batch-test", status, totalFiles, completedFiles, failedFiles, Optional.empty(),
                Optional.empty(), status.name().toLowerCase(), JsonPayload.empty(), Optional.empty(),
                Optional.empty(), now, now);
    }

    /**
     * 创建测试 OCR 结果。
     *
     * @param documentId 文档 ID
     * @param markdownStorageUri Markdown 存储地址
     * @return OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrResult result(String documentId, String markdownStorageUri) {
        return new OcrResult("result-" + documentId, documentId, "text", markdownStorageUri, Map.of(), Map.of(),
                List.of(), List.of(), List.of(), List.of(), 1.0, List.of(), OffsetDateTime.now());
    }

    /**
     * 创建测试文档事件。
     *
     * @param documentId 文档 ID
     * @param eventType 事件类型
     * @return 文档事件
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrEvent documentEvent(String documentId, String eventType) {
        return new OcrEvent("event-" + documentId + '-' + eventType, eventType, "batch-test", Optional.of(documentId),
                "failed", "ocr_images", Map.of("percent", 100), JsonPayload.empty(), Optional.empty(), Map.of(),
                Map.of(), OffsetDateTime.now());
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
            return documents.values().stream().limit(limit).toList();
        }

        @Override
        public void deleteById(String documentId) {
            documents.remove(documentId);
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
            if (batch == null) {
                return;
            } else {
                batches.put(batchId, new Batch(batch.batchId(), status,
                        Math.max(0, batch.totalFiles() - 1), completedFiles, failedFiles, batch.currentDocumentId(),
                        batch.currentDocumentName(), batch.currentStage(), batch.metadata(), batch.callbackUrl(),
                        batch.idempotencyKey(), batch.createdAt(), OffsetDateTime.now()));
            }
        }

        @Override
        public void deleteById(String batchId) {
            batches.remove(batchId);
        }
    }

    /**
     * 内存 OCR 结果仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class InMemoryOcrResultRepository implements OcrResultRepository {

        private final Map<String, OcrResult> results = new HashMap<>(TEST_CAPACITY);

        @Override
        public void save(OcrResult result) {
            results.put(result.documentId(), result);
        }

        @Override
        public void saveAll(List<OcrResult> results) {
            results.forEach(this::save);
        }

        @Override
        public Optional<OcrResult> findByDocumentId(String documentId) {
            return Optional.ofNullable(results.get(documentId));
        }

        @Override
        public void deleteByDocumentId(String documentId) {
            results.remove(documentId);
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

        @Override
        public void deleteByDocumentId(String documentId) {
            events.removeIf(event -> event.documentId().filter(documentId::equals).isPresent());
        }
    }

    /**
     * 记录删除请求的对象存储桩。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class RecordingObjectStorage implements ObjectStorage {

        private final List<String> deletedUris = new ArrayList<>(TEST_CAPACITY);

        @Override
        public String writeBytes(String objectKey, byte[] content) {
            return "local://" + objectKey;
        }

        @Override
        public byte[] readBytes(String storageUri) {
            return new byte[0];
        }

        @Override
        public void delete(String storageUri) {
            deletedUris.add(storageUri);
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
