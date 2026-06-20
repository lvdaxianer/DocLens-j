package io.github.lvdaxianer.doclens.j.processing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lvdaxianer.doclens.j.ingestion.application.BatchProcessingScheduler;
import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResult;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTask;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.PdfMode;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 文档重试用例测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class DocumentRetryUseCaseTest {

    private static final int TEST_CAPACITY = 4;

    /**
     * 失败文档重试后应重置状态、错误信息并重新调度所在批次。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void retryFailedDocumentResetsStateAndSchedulesBatch() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository();
        InMemoryOcrEventRepository eventRepository = new InMemoryOcrEventRepository();
        RecordingBatchProcessingScheduler scheduler = new RecordingBatchProcessingScheduler();
        OffsetDateTime now = OffsetDateTime.now();
        DocumentJob failedDocument = document("doc-failed", 0)
                .startProcessing(now)
                .advanceStage(ProcessingStage.OCR_IMAGES, 2, 5, now.plusSeconds(1))
                .fail(DocLensConstants.ERROR_CODE_OCR_FAILED, "ocr failed", now.plusSeconds(2));
        documentRepository.save(failedDocument);
        batchRepository.save(batch());
        DocumentRetryUseCase useCase = useCase(documentRepository, batchRepository, eventRepository, scheduler);

        useCase.retry("doc-failed");

        assertThat(documentRepository.findById("doc-failed")).get().satisfies(document -> {
            assertThat(document.status()).isEqualTo(DocumentStatus.QUEUED);
            assertThat(document.stage()).isEqualTo(ProcessingStage.QUEUED);
            assertThat(document.progressPercent()).isEqualTo(DocLensConstants.ZERO_PROGRESS_PERCENT);
            assertThat(document.currentPage()).isZero();
            assertThat(document.errorCode()).isEmpty();
            assertThat(document.errorMessage()).isEmpty();
            assertThat(document.resultId()).isEmpty();
        });
        assertThat(scheduler.scheduledBatchIds).containsExactly("batch-test");
        assertThat(eventRepository.events)
                .extracting(OcrEvent::eventType)
                .contains(DocLensConstants.EVENT_DOCUMENT_RETRIED);
    }

    /**
     * 卡死文档也应允许执行重试，便于人工恢复。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void retryStalledDocumentIsAllowed() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository();
        InMemoryOcrEventRepository eventRepository = new InMemoryOcrEventRepository();
        RecordingBatchProcessingScheduler scheduler = new RecordingBatchProcessingScheduler();
        OffsetDateTime now = OffsetDateTime.now();
        DocumentJob stalledDocument = new DocumentJob("doc-stalled", "batch-test", "doc-stalled.txt", DocumentType.TEXT,
                5, 1, "local://doc-stalled", DocumentStatus.STALLED, ProcessingStage.OCR_IMAGES, 50, 2, 5,
                "stub_ocr", Optional.<PdfMode>empty(), ChunkStrategy.general(),
                io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy.defaultPolicy(), JsonPayload.empty(),
                Optional.empty(), Optional.of("STALE_DOCUMENT"),
                Optional.of("stalled"), 0, now, now.plusSeconds(10));
        documentRepository.save(stalledDocument);
        batchRepository.save(batch());
        DocumentRetryUseCase useCase = useCase(documentRepository, batchRepository, eventRepository, scheduler);

        useCase.retry("doc-stalled");

        assertThat(documentRepository.findById("doc-stalled")).get().extracting(DocumentJob::status)
                .isEqualTo(DocumentStatus.QUEUED);
        assertThat(scheduler.scheduledBatchIds).containsExactly("batch-test");
    }

    /**
     * 已完成文档不应允许重试，避免破坏最终结果稳定性。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void retryCompletedDocumentIsRejected() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository();
        InMemoryOcrEventRepository eventRepository = new InMemoryOcrEventRepository();
        RecordingBatchProcessingScheduler scheduler = new RecordingBatchProcessingScheduler();
        OffsetDateTime now = OffsetDateTime.now();
        DocumentJob completedDocument = document("doc-completed", 0)
                .startProcessing(now)
                .complete("result-doc-completed", now.plusSeconds(1));
        documentRepository.save(completedDocument);
        batchRepository.save(batch());
        DocumentRetryUseCase useCase = useCase(documentRepository, batchRepository, eventRepository, scheduler);

        assertThatThrownBy(() -> useCase.retry("doc-completed"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("completed");
        assertThat(scheduler.scheduledBatchIds).isEmpty();
    }

    /**
     * 文档重试后必须清理旧的页任务和页结果，避免新一轮处理撞上唯一键。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void retryFailedDocumentClearsPageChildrenBeforeRescheduling() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository();
        InMemoryOcrEventRepository eventRepository = new InMemoryOcrEventRepository();
        InMemoryDocumentPageTaskRepository pageTaskRepository = new InMemoryDocumentPageTaskRepository();
        InMemoryDocumentPageResultRepository pageResultRepository = new InMemoryDocumentPageResultRepository();
        RecordingBatchProcessingScheduler scheduler = new RecordingBatchProcessingScheduler();
        OffsetDateTime now = OffsetDateTime.now();
        DocumentJob failedDocument = document("doc-failed", 0)
                .startProcessing(now)
                .advanceStage(ProcessingStage.OCR_IMAGES, 2, 5, now.plusSeconds(1))
                .fail(DocLensConstants.ERROR_CODE_OCR_FAILED, "ocr failed", now.plusSeconds(2));
        documentRepository.save(failedDocument);
        batchRepository.save(batch());
        pageTaskRepository.saveAll(List.of(pageTask("page-task-1", "doc-failed", 1),
                pageTask("page-task-2", "doc-failed", 2)));
        pageResultRepository.upsert(pageResult("doc-failed", 1));
        pageResultRepository.upsert(pageResult("doc-failed", 2));
        DocumentRetryUseCase useCase = new DocumentRetryUseCase(new DocumentRetryDependencies(documentRepository,
                batchRepository, eventRepository, scheduler, new OcrEventFactory(new IdGenerator()),
                new DocumentRetryCleanupDependencies(pageTaskRepository, pageResultRepository)),
                new InlineTransactionRunner());

        useCase.retry("doc-failed");

        assertThat(pageTaskRepository.listByDocumentId("doc-failed")).isEmpty();
        assertThat(pageResultRepository.listByDocumentId("doc-failed")).isEmpty();
        assertThat(scheduler.scheduledBatchIds).containsExactly("batch-test");
    }

    /**
     * 创建待测文档重试用例。
     *
     * @param documentRepository 文档仓储
     * @param batchRepository 批次仓储
     * @param eventRepository 事件仓储
     * @param scheduler 批次处理调度器
     * @return 文档重试用例
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentRetryUseCase useCase(
            InMemoryDocumentJobRepository documentRepository,
            InMemoryBatchRepository batchRepository,
            InMemoryOcrEventRepository eventRepository,
            RecordingBatchProcessingScheduler scheduler
    ) {
        return new DocumentRetryUseCase(new DocumentRetryDependencies(documentRepository, batchRepository,
                eventRepository, scheduler, new OcrEventFactory(new IdGenerator()),
                new DocumentRetryCleanupDependencies(new InMemoryDocumentPageTaskRepository(),
                        new InMemoryDocumentPageResultRepository())),
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
                JsonPayload.empty(), sortOrder, OffsetDateTime.now(), ChunkStrategy.general()));
    }

    /**
     * 创建测试批次。
     *
     * @return 测试批次
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private Batch batch() {
        return Batch.create("batch-test", 1, JsonPayload.empty(), Optional.empty(), Optional.empty(),
                OffsetDateTime.now());
    }

    /**
     * 创建测试页任务。
     *
     * @param taskId 任务 ID
     * @param documentId 文档 ID
     * @param pageNo 页码
     * @return 测试页任务
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private DocumentPageTask pageTask(String taskId, String documentId, int pageNo) {
        OffsetDateTime now = OffsetDateTime.now();
        return new DocumentPageTask(taskId, "batch-test", documentId, pageNo, "page://" + pageNo,
                io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskStatus.COMPLETED,
                Optional.empty(), Optional.empty(), 0, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(now), now, now);
    }

    /**
     * 创建测试页结果。
     *
     * @param documentId 文档 ID
     * @param pageNo 页码
     * @return 测试页结果
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private DocumentPageResult pageResult(String documentId, int pageNo) {
        OffsetDateTime now = OffsetDateTime.now();
        return new DocumentPageResult(documentId, pageNo, Map.of("page", pageNo), "page-" + pageNo, List.of(),
                1.0, List.of(), "node-" + pageNo, now, now);
    }

    /**
     * 内存页结果仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private static final class InMemoryDocumentPageResultRepository implements DocumentPageResultRepository {

        private final Map<String, DocumentPageResult> results = new HashMap<>(TEST_CAPACITY);

        @Override
        public void upsert(DocumentPageResult result) {
            results.put(key(result.documentId(), result.pageNo()), result);
        }

        @Override
        public Optional<DocumentPageResult> findByDocumentIdAndPageNo(String documentId, int pageNo) {
            return Optional.ofNullable(results.get(key(documentId, pageNo)));
        }

        @Override
        public List<DocumentPageResult> listByDocumentId(String documentId) {
            return results.values().stream().filter(result -> documentId.equals(result.documentId())).toList();
        }

        @Override
        public List<DocumentPageResult> listByDocumentIds(List<String> documentIds) {
            return results.values().stream().filter(result -> documentIds.contains(result.documentId())).toList();
        }

        @Override
        public void deleteByDocumentId(String documentId) {
            results.entrySet().removeIf(entry -> documentId.equals(entry.getValue().documentId()));
        }

        /**
         * 生成页结果唯一键。
         *
         * @param documentId 文档 ID
         * @param pageNo 页码
         * @return 结果键
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        private String key(String documentId, int pageNo) {
            return documentId + "#" + pageNo;
        }
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
    }

    /**
     * 内存页任务仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private static final class InMemoryDocumentPageTaskRepository implements DocumentPageTaskRepository {

        private final List<DocumentPageTask> tasks = new ArrayList<>(TEST_CAPACITY);

        @Override
        public void saveAll(List<DocumentPageTask> tasks) {
            this.tasks.addAll(tasks);
        }

        @Override
        public List<DocumentPageTask> listQueued(int limit) {
            return tasks.stream().limit(limit).toList();
        }

        @Override
        public List<DocumentPageTask> listProcessingExpired(OffsetDateTime now, int limit) {
            return List.of();
        }

        @Override
        public void updateAll(List<DocumentPageTask> tasks) {
            this.tasks.clear();
            this.tasks.addAll(tasks);
        }

        @Override
        public void deleteByDocumentId(String documentId) {
            tasks.removeIf(task -> documentId.equals(task.documentId()));
        }

        @Override
        public boolean tryMarkProcessing(io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskClaimRequest request) {
            return false;
        }

        @Override
        public void markCompleted(io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskCompletionRequest request) {
        }

        @Override
        public void markFailed(io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskFailureRequest request) {
        }

        @Override
        public List<DocumentPageTask> listByDocumentId(String documentId) {
            return tasks.stream().filter(task -> documentId.equals(task.documentId())).toList();
        }

        @Override
        public Optional<DocumentPageTask> findByDocumentIdAndPageNo(String documentId, int pageNo) {
            return tasks.stream().filter(task -> documentId.equals(task.documentId()))
                    .filter(task -> task.pageNo() == pageNo).findFirst();
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
                        failedFiles, batch.currentDocumentId(), batch.currentDocumentName(), batch.currentStage(),
                        batch.metadata(), batch.callbackUrl(), batch.idempotencyKey(), batch.createdAt(),
                        OffsetDateTime.now()));
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
     * 记录调度请求的批次调度器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class RecordingBatchProcessingScheduler implements BatchProcessingScheduler {

        private final List<String> scheduledBatchIds = new ArrayList<>(TEST_CAPACITY);

        @Override
        public void schedule(String batchId) {
            scheduledBatchIds.add(batchId);
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
