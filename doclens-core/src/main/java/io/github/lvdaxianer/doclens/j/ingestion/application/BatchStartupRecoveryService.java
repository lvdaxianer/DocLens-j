package io.github.lvdaxianer.doclens.j.ingestion.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.EventCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 批次启动恢复服务。
 *
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
public class BatchStartupRecoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchStartupRecoveryService.class);

    private final DocumentJobRepository documentRepository;
    private final BatchRepository batchRepository;
    private final DocumentPageTaskRepository pageTaskRepository;
    private final DocumentPageResultRepository pageResultRepository;
    private final OcrEventRepository eventRepository;
    private final OcrEventFactory eventFactory;
    private final BatchProcessingScheduler batchProcessingScheduler;
    private final TransactionRunner transactionRunner;

    /**
     * 创建批次启动恢复服务。
     *
     * @param dependencies 启动恢复依赖集合
     * @param transactionRunner 事务执行器
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    public BatchStartupRecoveryService(
            BatchStartupRecoveryDependencies dependencies,
            TransactionRunner transactionRunner
    ) {
        this.documentRepository = dependencies.documentRepository();
        this.batchRepository = dependencies.batchRepository();
        this.pageTaskRepository = dependencies.pageTaskRepository();
        this.pageResultRepository = dependencies.pageResultRepository();
        this.eventRepository = dependencies.eventRepository();
        this.eventFactory = dependencies.eventFactory();
        this.batchProcessingScheduler = dependencies.batchProcessingScheduler();
        this.transactionRunner = transactionRunner;
    }

    /**
     * 恢复仍有排队文档的批次。
     *
     * @param limit 最大恢复批次数量
     * @return 已调度批次数量
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    public int recoverQueuedBatches(int limit) {
        Set<String> batchIds = recoverableBatchIds(limit);
        recoverStalledDocuments(batchIds);
        batchIds.forEach(batchProcessingScheduler::schedule);
        LOGGER.info("[批次恢复] 启动恢复完成, recoveredBatchCount={}", batchIds.size());
        return batchIds.size();
    }

    /**
     * 查询可恢复批次 ID。
     *
     * @param limit 最大批次数量
     * @return 可恢复批次 ID
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private Set<String> recoverableBatchIds(int limit) {
        int safeLimit = Math.max(0, limit);
        Set<String> batchIds = new LinkedHashSet<>(safeLimit);
        batchIds.addAll(documentRepository.listQueuedBatchIds(safeLimit));
        if (batchIds.size() < safeLimit) {
            fillStalledBatchIds(batchIds, safeLimit);
        } else {
            // 已达到恢复批次数量上限，无需继续查询卡住批次。
        }
        return batchIds;
    }

    /**
     * 补充卡住文档所属批次 ID。
     *
     * @param batchIds 已收集的批次 ID
     * @param limit 最大批次数量
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private void fillStalledBatchIds(Set<String> batchIds, int limit) {
        List<String> stalledBatchIds = documentRepository.listBatchIdsByStatus(DocumentStatus.STALLED, limit);
        for (String batchId : stalledBatchIds) {
            addStalledBatchId(batchIds, batchId, limit);
        }
    }

    /**
     * 添加单个卡住批次 ID。
     *
     * @param batchIds 已收集的批次 ID
     * @param batchId 候选卡住批次 ID
     * @param limit 最大批次数量
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private void addStalledBatchId(Set<String> batchIds, String batchId, int limit) {
        if (batchIds.size() < limit) {
            batchIds.add(batchId);
        } else {
            // 已达到恢复批次数量上限，忽略后续卡住批次。
        }
    }

    /**
     * 恢复可自动重置的卡住文档。
     *
     * @param batchIds 可恢复批次 ID
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private void recoverStalledDocuments(Set<String> batchIds) {
        if (batchIds.isEmpty()) {
            // 没有可恢复批次时无需开启事务。
        } else {
            transactionRunner.requiredVoid(() -> resetStalledDocuments(batchIds));
        }
    }

    /**
     * 重置批次内卡住文档。
     *
     * @param batchIds 批次 ID 集合
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private void resetStalledDocuments(Set<String> batchIds) {
        List<DocumentJob> documents = documentRepository.listByBatchIds(batchIds.stream().toList());
        List<DocumentJob> resetDocuments = resetStalledDocuments(documents);
        documentRepository.updateAll(resetDocuments);
        eventRepository.saveAll(resetDocuments.stream().map(this::recoveredEvent).toList());
        refreshBatchSummaries(documents, resetDocuments);
    }

    /**
     * 重置卡住文档集合。
     *
     * @param documents 批次文档集合
     * @return 重置后的文档集合
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private List<DocumentJob> resetStalledDocuments(List<DocumentJob> documents) {
        return documents.stream()
                .filter(document -> document.status() == DocumentStatus.STALLED)
                .map(this::resetStalledDocument)
                .toList();
    }

    /**
     * 重置单个卡住文档。
     *
     * @param document 卡住文档
     * @return 重置后的文档
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private DocumentJob resetStalledDocument(DocumentJob document) {
        pageResultRepository.deleteByDocumentId(document.documentId());
        pageTaskRepository.deleteByDocumentId(document.documentId());
        return document.retry(OffsetDateTime.now());
    }

    /**
     * 构建启动恢复事件。
     *
     * @param document 重置后的文档
     * @return OCR 事件
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private OcrEvent recoveredEvent(DocumentJob document) {
        return eventFactory.create(new EventCreateRequest(document.batchId(), Optional.of(document.documentId()),
                DocLensConstants.EVENT_DOCUMENT_RETRIED, document.status().name().toLowerCase(),
                document.stage().name().toLowerCase(),
                Map.of("percent", document.progressPercent(), "current_page", document.currentPage(),
                        "total_pages", document.totalPages()),
                document.metadata(), Optional.empty(), Map.of("reason", "startup_recovery"), Map.of()));
    }

    /**
     * 刷新批次摘要集合。
     *
     * @param originalDocuments 原始文档集合
     * @param resetDocuments 重置后文档集合
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private void refreshBatchSummaries(List<DocumentJob> originalDocuments, List<DocumentJob> resetDocuments) {
        Map<String, DocumentJob> resetByDocumentId = resetDocuments.stream()
                .collect(Collectors.toMap(DocumentJob::documentId, document -> document));
        Map<String, List<DocumentJob>> documentsByBatch = originalDocuments.stream()
                .map(document -> resetByDocumentId.getOrDefault(document.documentId(), document))
                .collect(Collectors.groupingBy(DocumentJob::batchId));
        documentsByBatch.forEach(this::refreshBatchSummary);
    }

    /**
     * 刷新单个批次摘要。
     *
     * @param batchId 批次 ID
     * @param documents 批次文档
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private void refreshBatchSummary(String batchId, List<DocumentJob> documents) {
        int completedCount = Math.toIntExact(documents.stream()
                .filter(document -> document.status() == DocumentStatus.COMPLETED)
                .count());
        int failedCount = Math.toIntExact(documents.stream().filter(document -> document.status().isFailureLike()).count());
        batchRepository.updateSummary(batchId, completedCount, failedCount, batchStatus(documents));
    }

    /**
     * 计算批次状态。
     *
     * @param documents 批次文档
     * @return 批次状态
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private BatchStatus batchStatus(List<DocumentJob> documents) {
        long queuedCount = documents.stream().filter(document -> document.status() == DocumentStatus.QUEUED).count();
        long processingCount = documents.stream()
                .filter(document -> document.status() == DocumentStatus.PROCESSING)
                .count();
        long completedCount = documents.stream()
                .filter(document -> document.status() == DocumentStatus.COMPLETED)
                .count();
        long failedCount = documents.stream().filter(document -> document.status().isFailureLike()).count();
        if (queuedCount > 0 || processingCount > 0) {
            return BatchStatus.PROCESSING;
        } else if (failedCount > 0 && completedCount > 0) {
            return BatchStatus.PARTIAL_FAILED;
        } else if (failedCount > 0) {
            return BatchStatus.FAILED;
        } else if (completedCount == documents.size()) {
            return BatchStatus.COMPLETED;
        } else {
            return BatchStatus.QUEUED;
        }
    }
}
