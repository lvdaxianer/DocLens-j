package io.github.lvdaxianer.doclens.j.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResult;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTask;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskClaimRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskCompletionRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskFailureRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskRepository;
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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 批次启动恢复服务测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
class BatchStartupRecoveryServiceTest {

    private static final int RECOVERY_LIMIT = 2;
    private static final int PAGE_COUNT = 1;
    private static final long FILE_SIZE = 8L;

    /**
     * 启动恢复应按批次去重调度仍有排队文档的批次。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void recoverQueuedBatchesSchedulesDistinctQueuedBatchIds() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        RecordingBatchProcessingScheduler scheduler = new RecordingBatchProcessingScheduler();
        BatchStartupRecoveryService service = recoveryService(documentRepository, scheduler);
        documentRepository.save(queuedDocument("doc-1", "batch-1", 0));
        documentRepository.save(queuedDocument("doc-2", "batch-1", 1));
        documentRepository.save(queuedDocument("doc-3", "batch-2", 2));

        int recovered = service.recoverQueuedBatches(RECOVERY_LIMIT);

        assertThat(recovered).isEqualTo(RECOVERY_LIMIT);
        assertThat(scheduler.batchIds).containsExactly("batch-1", "batch-2");
    }

    /**
     * 已完成和失败文档不应在启动恢复中自动重试。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void recoverQueuedBatchesSkipsCompletedAndFailedOnlyBatches() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        RecordingBatchProcessingScheduler scheduler = new RecordingBatchProcessingScheduler();
        BatchStartupRecoveryService service = recoveryService(documentRepository, scheduler);
        OffsetDateTime now = OffsetDateTime.now();
        documentRepository.save(queuedDocument("doc-completed", "batch-completed", 0)
                .complete("result-1", now));
        documentRepository.save(queuedDocument("doc-failed", "batch-failed", 1)
                .fail("OCR_FAILED", "failed", now));

        int recovered = service.recoverQueuedBatches(RECOVERY_LIMIT);

        assertThat(recovered).isZero();
        assertThat(scheduler.batchIds).isEmpty();
    }

    /**
     * 启动恢复应受 limit 限制，避免启动瞬间提交过多批次。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void recoverQueuedBatchesHonorsLimit() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        RecordingBatchProcessingScheduler scheduler = new RecordingBatchProcessingScheduler();
        BatchStartupRecoveryService service = recoveryService(documentRepository, scheduler);
        documentRepository.save(queuedDocument("doc-1", "batch-1", 0));
        documentRepository.save(queuedDocument("doc-2", "batch-2", 1));
        documentRepository.save(queuedDocument("doc-3", "batch-3", 2));

        int recovered = service.recoverQueuedBatches(RECOVERY_LIMIT);

        assertThat(recovered).isEqualTo(RECOVERY_LIMIT);
        assertThat(scheduler.batchIds).containsExactly("batch-1", "batch-2");
    }

    /**
     * 卡住批次查询与排队批次重叠时仍应补满恢复上限。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void recoverQueuedBatchesFillsLimitWhenQueuedAndStalledBatchIdsOverlap() {
        RecoveryFixture fixture = recoveryFixture();
        OffsetDateTime now = OffsetDateTime.now();
        fixture.batchRepository.save(batch("batch-1", BatchStatus.PROCESSING, 2, 0, 1, now));
        fixture.batchRepository.save(batch("batch-2", BatchStatus.PROCESSING, 1, 0, 1, now));
        fixture.documentRepository.save(queuedDocument("doc-queued", "batch-1", 0));
        fixture.documentRepository.save(queuedDocument("doc-stalled-1", "batch-1", 1)
                .stall("STALE", "stalled", now));
        fixture.documentRepository.save(queuedDocument("doc-stalled-2", "batch-2", 2)
                .stall("STALE", "stalled", now));

        int recovered = fixture.service.recoverQueuedBatches(RECOVERY_LIMIT);

        assertThat(recovered).isEqualTo(RECOVERY_LIMIT);
        assertThat(fixture.scheduler.batchIds).containsExactly("batch-1", "batch-2");
    }

    /**
     * 启动恢复应将卡住文档重置为排队并调度其批次。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void recoverQueuedBatchesResetsStalledDocumentsBeforeScheduling() {
        RecoveryFixture fixture = recoveryFixture();
        OffsetDateTime now = OffsetDateTime.now();
        fixture.batchRepository.save(batch("batch-stalled", BatchStatus.PROCESSING, 1, 0, 1, now));
        fixture.documentRepository.save(queuedDocument("doc-stalled", "batch-stalled", 0)
                .markOcrQueued(PAGE_COUNT, now)
                .stall("STALE", "stalled", now.plusSeconds(1)));
        fixture.pageTaskRepository.saveDeletedProbe("doc-stalled");
        fixture.pageResultRepository.saveDeletedProbe("doc-stalled");

        int recovered = fixture.service.recoverQueuedBatches(RECOVERY_LIMIT);

        assertThat(recovered).isEqualTo(1);
        assertThat(fixture.scheduler.batchIds).containsExactly("batch-stalled");
        assertThat(fixture.documentRepository.findById("doc-stalled")).get()
                .extracting(DocumentJob::status, DocumentJob::stage, DocumentJob::progressPercent)
                .containsExactly(DocumentStatus.QUEUED, ProcessingStage.QUEUED, 0);
        assertThat(fixture.pageTaskRepository.deletedDocumentIds).containsExactly("doc-stalled");
        assertThat(fixture.pageResultRepository.deletedDocumentIds).containsExactly("doc-stalled");
        assertThat(fixture.eventRepository.events).extracting(OcrEvent::eventType)
                .contains(DocLensConstants.EVENT_DOCUMENT_RETRIED);
        assertThat(fixture.batchRepository.findById("batch-stalled")).get()
                .extracting(Batch::status, Batch::completedFiles, Batch::failedFiles)
                .containsExactly(BatchStatus.PROCESSING, 0, 0);
    }

    /**
     * 创建排队文档。
     *
     * @param documentId 文档 ID
     * @param batchId 批次 ID
     * @param sortOrder 上传顺序
     * @return 排队文档
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private DocumentJob queuedDocument(String documentId, String batchId, int sortOrder) {
        return DocumentJob.create(new DocumentJobCreateRequest(documentId, batchId, documentId + ".txt",
                DocumentType.TEXT, FILE_SIZE, PAGE_COUNT, "local://" + documentId, "stub_ocr", Optional.empty(),
                JsonPayload.empty(), sortOrder, OffsetDateTime.now()));
    }

    /**
     * 创建测试恢复服务。
     *
     * @param documentRepository 文档仓储
     * @param scheduler 调度器
     * @return 启动恢复服务
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private BatchStartupRecoveryService recoveryService(
            InMemoryDocumentJobRepository documentRepository,
            RecordingBatchProcessingScheduler scheduler
    ) {
        return recoveryFixture(documentRepository, scheduler).service;
    }

    /**
     * 创建完整恢复测试夹具。
     *
     * @return 恢复测试夹具
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private RecoveryFixture recoveryFixture() {
        return recoveryFixture(new InMemoryDocumentJobRepository(), new RecordingBatchProcessingScheduler());
    }

    /**
     * 创建完整恢复测试夹具。
     *
     * @param documentRepository 文档仓储
     * @param scheduler 调度器
     * @return 恢复测试夹具
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private RecoveryFixture recoveryFixture(
            InMemoryDocumentJobRepository documentRepository,
            RecordingBatchProcessingScheduler scheduler
    ) {
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository();
        RecordingPageTaskRepository pageTaskRepository = new RecordingPageTaskRepository();
        RecordingPageResultRepository pageResultRepository = new RecordingPageResultRepository();
        RecordingOcrEventRepository eventRepository = new RecordingOcrEventRepository();
        OcrEventFactory eventFactory = new OcrEventFactory(new IdGenerator());
        BatchStartupRecoveryDependencies dependencies = new BatchStartupRecoveryDependencies(documentRepository,
                batchRepository, pageTaskRepository, pageResultRepository, eventRepository, eventFactory, scheduler);
        BatchStartupRecoveryService service = new BatchStartupRecoveryService(dependencies, new ImmediateTransactionRunner());
        return new RecoveryFixture(service, documentRepository, batchRepository, pageTaskRepository,
                pageResultRepository, eventRepository, scheduler);
    }

    /**
     * 创建测试批次。
     *
     * @param batchId 批次 ID
     * @param status 批次状态
     * @param totalFiles 总文件数
     * @param completedFiles 已完成文件数
     * @param failedFiles 失败文件数
     * @param now 当前时间
     * @return 批次
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private Batch batch(
            String batchId,
            BatchStatus status,
            int totalFiles,
            int completedFiles,
            int failedFiles,
            OffsetDateTime now
    ) {
        return new Batch(batchId, status, totalFiles, completedFiles, failedFiles, Optional.empty(),
                Optional.empty(), status.name().toLowerCase(), JsonPayload.empty(), Optional.empty(),
                Optional.empty(), now, now);
    }

    /**
     * 恢复测试夹具。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private record RecoveryFixture(
            BatchStartupRecoveryService service,
            InMemoryDocumentJobRepository documentRepository,
            InMemoryBatchRepository batchRepository,
            RecordingPageTaskRepository pageTaskRepository,
            RecordingPageResultRepository pageResultRepository,
            RecordingOcrEventRepository eventRepository,
            RecordingBatchProcessingScheduler scheduler
    ) {
    }

    /**
     * 记录批次调度调用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private static class RecordingBatchProcessingScheduler implements BatchProcessingScheduler {

        private final List<String> batchIds = new ArrayList<>(RECOVERY_LIMIT);

        @Override
        public void schedule(String batchId) {
            batchIds.add(batchId);
        }
    }

    /**
     * 内存文档任务仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private static class InMemoryDocumentJobRepository implements DocumentJobRepository {

        private final Map<String, DocumentJob> documents = new LinkedHashMap<>(RECOVERY_LIMIT);

        /**
         * 保存文档任务。
         *
         * @param document 文档任务
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public void save(DocumentJob document) {
            documents.put(document.documentId(), document);
        }

        /**
         * 批量保存文档任务。
         *
         * @param documents 文档任务集合
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public void saveAll(List<DocumentJob> documents) {
            documents.forEach(this::save);
        }

        /**
         * 更新文档任务。
         *
         * @param document 文档任务
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public void update(DocumentJob document) {
            documents.put(document.documentId(), document);
        }

        /**
         * 批量更新文档任务。
         *
         * @param documents 文档任务集合
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public void updateAll(List<DocumentJob> documents) {
            documents.forEach(this::update);
        }

        /**
         * 按 ID 查询文档任务。
         *
         * @param documentId 文档 ID
         * @return 文档任务
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public Optional<DocumentJob> findById(String documentId) {
            return Optional.ofNullable(documents.get(documentId));
        }

        /**
         * 按批次 ID 查询文档任务。
         *
         * @param batchId 批次 ID
         * @return 文档任务集合
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public List<DocumentJob> listByBatchId(String batchId) {
            return documents.values().stream().filter(document -> batchId.equals(document.batchId())).toList();
        }

        /**
         * 按批次 ID 集合查询文档任务。
         *
         * @param batchIds 批次 ID 集合
         * @return 文档任务集合
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public List<DocumentJob> listByBatchIds(List<String> batchIds) {
            return documents.values().stream().filter(document -> batchIds.contains(document.batchId())).toList();
        }

        /**
         * 查询最近文档任务。
         *
         * @param limit 最大数量
         * @return 文档任务集合
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public List<DocumentJob> listRecent(int limit) {
            return documents.values().stream().limit(limit).toList();
        }

        /**
         * 查询仍有排队文档的批次 ID。
         *
         * @param limit 最大批次数量
         * @return 去重后的批次 ID 集合
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public List<String> listQueuedBatchIds(int limit) {
            return documents.values().stream()
                    .filter(document -> document.status() == DocumentStatus.QUEUED)
                    .map(DocumentJob::batchId)
                    .distinct()
                    .limit(limit)
                    .toList();
        }
    }

    /**
     * 内存批次仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private static class InMemoryBatchRepository implements BatchRepository {

        private final Map<String, Batch> batches = new LinkedHashMap<>(RECOVERY_LIMIT);

        /**
         * 保存批次。
         *
         * @param batch 批次
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public void save(Batch batch) {
            batches.put(batch.batchId(), batch);
        }

        /**
         * 根据 ID 查找批次。
         *
         * @param batchId 批次 ID
         * @return 批次
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public Optional<Batch> findById(String batchId) {
            return Optional.ofNullable(batches.get(batchId));
        }

        /**
         * 根据幂等键查找批次。
         *
         * @param idempotencyKey 幂等键
         * @return 批次
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public Optional<Batch> findByIdempotencyKey(String idempotencyKey) {
            return Optional.empty();
        }

        /**
         * 查询最近批次。
         *
         * @param limit 最大数量
         * @return 批次集合
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public List<Batch> listRecent(int limit) {
            return batches.values().stream().limit(limit).toList();
        }

        /**
         * 更新批次摘要。
         *
         * @param batchId 批次 ID
         * @param completedFiles 已完成文件数
         * @param failedFiles 失败文件数
         * @param status 批次状态
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public void updateSummary(String batchId, int completedFiles, int failedFiles, BatchStatus status) {
            Batch batch = batches.get(batchId);
            if (batch == null) {
                // 测试场景只更新已保存批次。
            } else {
                batches.put(batchId, new Batch(batch.batchId(), status, batch.totalFiles(), completedFiles,
                        failedFiles, batch.currentDocumentId(), batch.currentDocumentName(), batch.currentStage(),
                        batch.metadata(), batch.callbackUrl(), batch.idempotencyKey(), batch.createdAt(),
                        batch.updatedAt(), batch.callerIdentity()));
            }
        }
    }

    /**
     * 记录删除调用的页任务仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private static class RecordingPageTaskRepository implements DocumentPageTaskRepository {

        private final List<String> deletedDocumentIds = new ArrayList<>(RECOVERY_LIMIT);

        /**
         * 保存删除探针。
         *
         * @param documentId 文档 ID
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        void saveDeletedProbe(String documentId) {
            // 删除探针仅用于表达该文档预期存在旧页任务。
        }

        @Override
        public void saveAll(List<DocumentPageTask> tasks) {
            throw new UnsupportedOperationException("unused");
        }

        @Override
        public List<DocumentPageTask> listQueued(int limit) {
            return List.of();
        }

        @Override
        public List<DocumentPageTask> listProcessingExpired(OffsetDateTime now, int limit) {
            return List.of();
        }

        @Override
        public void updateAll(List<DocumentPageTask> tasks) {
            throw new UnsupportedOperationException("unused");
        }

        @Override
        public void deleteByDocumentId(String documentId) {
            deletedDocumentIds.add(documentId);
        }

        @Override
        public boolean tryMarkProcessing(DocumentPageTaskClaimRequest request) {
            return false;
        }

        @Override
        public void markCompleted(DocumentPageTaskCompletionRequest request) {
            throw new UnsupportedOperationException("unused");
        }

        @Override
        public void markFailed(DocumentPageTaskFailureRequest request) {
            throw new UnsupportedOperationException("unused");
        }

        @Override
        public List<DocumentPageTask> listByDocumentId(String documentId) {
            return List.of();
        }

        @Override
        public Optional<DocumentPageTask> findByDocumentIdAndPageNo(String documentId, int pageNo) {
            return Optional.empty();
        }
    }

    /**
     * 记录删除调用的页结果仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private static class RecordingPageResultRepository implements DocumentPageResultRepository {

        private final List<String> deletedDocumentIds = new ArrayList<>(RECOVERY_LIMIT);

        /**
         * 保存删除探针。
         *
         * @param documentId 文档 ID
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        void saveDeletedProbe(String documentId) {
            // 删除探针仅用于表达该文档预期存在旧页结果。
        }

        @Override
        public void upsert(DocumentPageResult result) {
            throw new UnsupportedOperationException("unused");
        }

        @Override
        public Optional<DocumentPageResult> findByDocumentIdAndPageNo(String documentId, int pageNo) {
            return Optional.empty();
        }

        @Override
        public List<DocumentPageResult> listByDocumentId(String documentId) {
            return List.of();
        }

        @Override
        public List<DocumentPageResult> listByDocumentIds(List<String> documentIds) {
            return List.of();
        }

        @Override
        public void deleteByDocumentId(String documentId) {
            deletedDocumentIds.add(documentId);
        }
    }

    /**
     * 记录 OCR 事件的仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private static class RecordingOcrEventRepository implements OcrEventRepository {

        private final List<OcrEvent> events = new ArrayList<>(RECOVERY_LIMIT);

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
     * 立即执行的事务 runner。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private static class ImmediateTransactionRunner implements TransactionRunner {

        /**
         * 立即执行有返回值动作。
         *
         * @param action 事务动作
         * @param <T> 结果类型
         * @return 动作结果
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public <T> T requiredResult(java.util.function.Supplier<T> action) {
            return action.get();
        }

        /**
         * 立即执行无返回值动作。
         *
         * @param action 事务动作
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public void requiredVoid(Runnable action) {
            action.run();
        }
    }
}
