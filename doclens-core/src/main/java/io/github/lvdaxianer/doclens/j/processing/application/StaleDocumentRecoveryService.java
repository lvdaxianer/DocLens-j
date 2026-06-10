package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.EventCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 扫描并回收长时间无进展的处理中任务。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public class StaleDocumentRecoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaleDocumentRecoveryService.class);
    private static final String STALE_REASON = "stale_progress_detected";

    private final DocumentJobRepository documentRepository;
    private final BatchRepository batchRepository;
    private final OcrEventRepository eventRepository;
    private final OcrEventFactory eventFactory;
    private final TransactionRunner transactionRunner;
    private final Duration staleThreshold;

    /**
     * 创建卡死文档恢复服务。
     *
     * @param dependencies 恢复依赖
     * @param transactionRunner 事务执行器
     * @param staleThreshold stale 阈值
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public StaleDocumentRecoveryService(
            StaleDocumentRecoveryDependencies dependencies,
            TransactionRunner transactionRunner,
            Duration staleThreshold
    ) {
        StaleDocumentRecoveryDependencies requiredDependencies = Objects.requireNonNull(dependencies,
                "dependencies must not be null");
        this.documentRepository = requiredDependencies.documentRepository();
        this.batchRepository = requiredDependencies.batchRepository();
        this.eventRepository = requiredDependencies.eventRepository();
        this.eventFactory = requiredDependencies.eventFactory();
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner must not be null");
        this.staleThreshold = Objects.requireNonNull(staleThreshold, "staleThreshold must not be null");
    }

    /**
     * 将超过阈值仍无进展的处理中任务转为已卡住。
     *
     * @param now 当前时间
     * @return 本轮标记为卡住的文档数量
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public int markStaleDocuments(OffsetDateTime now) {
        return transactionRunner.requiredResult(() -> markStaleDocumentsWithinTransaction(now));
    }

    /**
     * 在事务内执行 stale 扫描、状态更新和批次摘要刷新。
     *
     * @param now 当前时间
     * @return 本轮恢复数量
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private int markStaleDocumentsWithinTransaction(OffsetDateTime now) {
        List<DocumentJob> staleDocuments = staleDocuments(now);
        if (staleDocuments.isEmpty()) {
            return 0;
        } else {
            List<DocumentJob> stalledDocuments = staleDocuments.stream()
                    .map(document -> document.stall(DocLensConstants.ERROR_CODE_STALE_DOCUMENT,
                            staleMessage(document, now), now))
                    .toList();
            documentRepository.updateAll(stalledDocuments);
            eventRepository.saveAll(stalledDocuments.stream().map(this::stalledEvent).toList());
            refreshBatchSummaries(batchIds(stalledDocuments));
            LOGGER.warn("[文档恢复] 标记卡死文档完成, count={}, thresholdSeconds={}", stalledDocuments.size(),
                    staleThreshold.getSeconds());
            return stalledDocuments.size();
        }
    }

    /**
     * 查找超过阈值的处理中任务。
     *
     * @param now 当前时间
     * @return stale 文档集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private List<DocumentJob> staleDocuments(OffsetDateTime now) {
        return documentRepository.listByStatus(DocumentStatus.PROCESSING, DocLensConstants.DEFAULT_QUERY_LIMIT).stream()
                .filter(document -> isStale(document, now))
                .toList();
    }

    /**
     * 判断文档是否超过无进展阈值。
     *
     * @param document 文档任务
     * @param now 当前时间
     * @return 是否 stale
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private boolean isStale(DocumentJob document, OffsetDateTime now) {
        return document.updatedAt().plus(staleThreshold).isBefore(now);
    }

    /**
     * 刷新受影响批次的摘要状态。
     *
     * @param batchIds 受影响批次 ID 集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void refreshBatchSummaries(List<String> batchIds) {
        batchIds.forEach(this::refreshBatchSummary);
    }

    /**
     * 刷新单个批次的完成数、失败数和状态。
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
        int failedCount = Math.toIntExact(documents.stream().filter(document -> document.status().isFailureLike()).count());
        batchRepository.updateSummary(batchId, completedCount, failedCount, batchStatus(documents));
    }

    /**
     * 计算卡死恢复后的批次状态。
     *
     * @param documents 批次文档集合
     * @return 批次状态
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
        long failedCount = documents.stream().filter(document -> document.status().isFailureLike()).count();
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
     * 构建卡死原因描述，便于前端提示和事件追踪。
     *
     * @param document 文档任务
     * @param now 当前时间
     * @return 卡死原因文本
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private String staleMessage(DocumentJob document, OffsetDateTime now) {
        long stalledSeconds = Duration.between(document.updatedAt(), now).getSeconds();
        return "stale document detected after %d seconds without progress".formatted(stalledSeconds);
    }

    /**
     * 提取受影响批次 ID，避免重复刷新同一批次摘要。
     *
     * @param documents stale 文档集合
     * @return 去重后的批次 ID 集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private List<String> batchIds(List<DocumentJob> documents) {
        LinkedHashSet<String> batchIds = new LinkedHashSet<>(documents.size());
        documents.forEach(document -> batchIds.add(document.batchId()));
        return List.copyOf(batchIds);
    }

    /**
     * 构建文档卡死事件。
     *
     * @param document 已标记为卡死的文档
     * @return 事件对象
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrEvent stalledEvent(DocumentJob document) {
        return eventFactory.create(new EventCreateRequest(document.batchId(), Optional.of(document.documentId()),
                DocLensConstants.EVENT_DOCUMENT_STALLED, document.status().name().toLowerCase(),
                document.stage().name().toLowerCase(),
                Map.of("percent", document.progressPercent(), "current_page", document.currentPage(),
                        "total_pages", document.totalPages()),
                document.metadata(), document.resultId(), Map.of("reason", STALE_REASON,
                        "stale_threshold_seconds", staleThreshold.getSeconds()),
                Map.of("code", DocLensConstants.ERROR_CODE_STALE_DOCUMENT,
                        "message", document.errorMessage().orElse(DocLensConstants.EMPTY_VALUE))));
    }
}
