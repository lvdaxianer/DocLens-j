package io.github.lvdaxianer.doclens.j.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
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
        BatchStartupRecoveryService service = new BatchStartupRecoveryService(documentRepository, scheduler);
        documentRepository.save(queuedDocument("doc-1", "batch-1", 0));
        documentRepository.save(queuedDocument("doc-2", "batch-1", 1));
        documentRepository.save(queuedDocument("doc-3", "batch-2", 2));

        int recovered = service.recoverQueuedBatches(RECOVERY_LIMIT);

        assertThat(recovered).isEqualTo(RECOVERY_LIMIT);
        assertThat(scheduler.batchIds).containsExactly("batch-1", "batch-2");
    }

    /**
     * 已完成、失败和卡死文档不应在启动恢复中自动重试。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void recoverQueuedBatchesSkipsTerminalAndStalledOnlyBatches() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        RecordingBatchProcessingScheduler scheduler = new RecordingBatchProcessingScheduler();
        BatchStartupRecoveryService service = new BatchStartupRecoveryService(documentRepository, scheduler);
        OffsetDateTime now = OffsetDateTime.now();
        documentRepository.save(queuedDocument("doc-completed", "batch-completed", 0)
                .complete("result-1", now));
        documentRepository.save(queuedDocument("doc-failed", "batch-failed", 1)
                .fail("OCR_FAILED", "failed", now));
        documentRepository.save(queuedDocument("doc-stalled", "batch-stalled", 2)
                .stall("STALE", "stalled", now));

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
        BatchStartupRecoveryService service = new BatchStartupRecoveryService(documentRepository, scheduler);
        documentRepository.save(queuedDocument("doc-1", "batch-1", 0));
        documentRepository.save(queuedDocument("doc-2", "batch-2", 1));
        documentRepository.save(queuedDocument("doc-3", "batch-3", 2));

        int recovered = service.recoverQueuedBatches(RECOVERY_LIMIT);

        assertThat(recovered).isEqualTo(RECOVERY_LIMIT);
        assertThat(scheduler.batchIds).containsExactly("batch-1", "batch-2");
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
}
