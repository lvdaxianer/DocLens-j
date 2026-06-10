package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.ingestion.application.BatchProcessingScheduler;
import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.EventCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.domain.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 文档级重试用例。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public class DocumentRetryUseCase {

    private static final String RETRY_REASON = "manual_retry";

    private final DocumentJobRepository documentRepository;
    private final BatchRepository batchRepository;
    private final OcrEventRepository eventRepository;
    private final BatchProcessingScheduler batchProcessingScheduler;
    private final OcrEventFactory eventFactory;
    private final TransactionRunner transactionRunner;

    /**
     * 创建文档级重试用例。
     *
     * @param dependencies 重试依赖
     * @param transactionRunner 事务执行器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public DocumentRetryUseCase(
            DocumentRetryDependencies dependencies,
            TransactionRunner transactionRunner
    ) {
        this.documentRepository = dependencies.documentRepository();
        this.batchRepository = dependencies.batchRepository();
        this.eventRepository = dependencies.eventRepository();
        this.batchProcessingScheduler = dependencies.batchProcessingScheduler();
        this.eventFactory = dependencies.eventFactory();
        this.transactionRunner = transactionRunner;
    }

    /**
     * 重试指定文档，并重新调度所在批次。
     *
     * @param documentId 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public void retry(String documentId) {
        String batchId = transactionRunner.requiredResult(() -> retryWithinTransaction(documentId));
        batchProcessingScheduler.schedule(batchId);
    }

    /**
     * 在事务内重置文档状态、写事件并刷新批次摘要。
     *
     * @param documentId 文档 ID
     * @return 被调度的批次 ID
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private String retryWithinTransaction(String documentId) {
        DocumentJob document = loadDocument(documentId);
        Batch batch = loadBatch(document.batchId());
        validateRetryable(document);
        DocumentJob retried = document.retry(OffsetDateTime.now());
        documentRepository.update(retried);
        eventRepository.save(eventFactory.create(retryEvent(batch, retried)));
        refreshBatchSummary(batch.batchId());
        return batch.batchId();
    }

    /**
     * 加载待重试文档。
     *
     * @param documentId 文档 ID
     * @return 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentJob loadDocument(String documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("document " + documentId + " not found"));
    }

    /**
     * 加载文档所属批次。
     *
     * @param batchId 批次 ID
     * @return 批次聚合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private Batch loadBatch(String batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("batch " + batchId + " not found"));
    }

    /**
     * 校验文档当前状态是否允许重试。
     *
     * @param document 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void validateRetryable(DocumentJob document) {
        if (document.status() == DocumentStatus.FAILED || document.status() == DocumentStatus.STALLED) {
            // 失败和卡死文档允许人工重试。
        } else {
            throw new IllegalArgumentException("document " + document.documentId() + " is not retryable from status "
                    + document.status().name().toLowerCase());
        }
    }

    /**
     * 刷新批次摘要，保证公开查询接口立即看到最新状态。
     *
     * @param batchId 批次 ID
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void refreshBatchSummary(String batchId) {
        List<DocumentJob> documents = documentRepository.listByBatchId(batchId);
        int completedCount = Math.toIntExact(documents.stream()
                .filter(document -> document.status() == DocumentStatus.COMPLETED)
                .count());
        int failedCount = Math.toIntExact(documents.stream()
                .filter(document -> document.status() == DocumentStatus.FAILED)
                .count());
        batchRepository.updateSummary(batchId, completedCount, failedCount, batchStatus(documents));
    }

    /**
     * 计算批次摘要状态。
     *
     * @param documents 批次文档集合
     * @return 批次摘要状态
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private BatchStatus batchStatus(List<DocumentJob> documents) {
        long queuedCount = documents.stream().filter(document -> document.status() == DocumentStatus.QUEUED).count();
        long processingCount = documents.stream()
                .filter(document -> document.status() == DocumentStatus.PROCESSING)
                .count();
        long completedCount = documents.stream()
                .filter(document -> document.status() == DocumentStatus.COMPLETED)
                .count();
        long failedCount = documents.stream().filter(document -> document.status() == DocumentStatus.FAILED).count();
        if (queuedCount > 0 || processingCount > 0) {
            return BatchStatus.PROCESSING;
        }
        if (failedCount > 0 && completedCount > 0) {
            return BatchStatus.PARTIAL_FAILED;
        }
        if (failedCount > 0) {
            return BatchStatus.FAILED;
        }
        if (completedCount == documents.size()) {
            return BatchStatus.COMPLETED;
        }
        return BatchStatus.QUEUED;
    }

    /**
     * 构建文档重试事件。
     *
     * @param batch 批次聚合
     * @param document 重置后的文档任务
     * @return 事件创建请求
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private EventCreateRequest retryEvent(Batch batch, DocumentJob document) {
        return new EventCreateRequest(batch.batchId(), Optional.of(document.documentId()),
                DocLensConstants.EVENT_DOCUMENT_RETRIED, document.status().name().toLowerCase(),
                document.stage().name().toLowerCase(),
                Map.of("percent", document.progressPercent(), "current_page", document.currentPage(),
                        "total_pages", document.totalPages()),
                document.metadata(), Optional.empty(), Map.of("reason", RETRY_REASON), Map.of());
    }
}
