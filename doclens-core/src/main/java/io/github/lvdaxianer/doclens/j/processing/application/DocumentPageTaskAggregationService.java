package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResult;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTask;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文档页任务聚合服务。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
public class DocumentPageTaskAggregationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentPageTaskAggregationService.class);

    private final DocumentJobRepository documentRepository;
    private final BatchRepository batchRepository;
    private final DocumentPageTaskRepository pageTaskRepository;
    private final DocumentPageResultRepository pageResultRepository;
    private final OcrResultRepository resultRepository;
    private final DocumentCompletionCallbackRecorder completionCallbackRecorder;
    private final TransactionRunner transactionRunner;
    private final DocumentPageTaskOcrResultBuilder resultBuilder;
    /**
     * 创建文档页任务聚合服务。
     *
     * @param dependencies 聚合依赖
     * @param transactionRunner 事务执行器
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    public DocumentPageTaskAggregationService(DocumentPageTaskAggregationDependencies dependencies,
            TransactionRunner transactionRunner) {
        this.documentRepository = dependencies.documentRepository();
        this.batchRepository = dependencies.batchRepository();
        this.pageTaskRepository = dependencies.pageTaskRepository();
        this.pageResultRepository = dependencies.pageResultRepository();
        this.resultRepository = dependencies.resultRepository();
        this.completionCallbackRecorder = new DocumentCompletionCallbackRecorder(dependencies);
        this.transactionRunner = transactionRunner;
        this.resultBuilder = new DocumentPageTaskOcrResultBuilder(dependencies);
    }

    /**
     * 记录单页 OCR 成功并在全部页完成后聚合文档结果。
     *
     * @param task 已完成页任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    public void recordSuccess(DocumentPageTask task) {
        transactionRunner.requiredResult(() -> recordSuccessInTransaction(task))
                .ifPresent(this::completeDocument);
    }

    /**
     * 在事务中记录页成功。
     *
     * @param task 已完成页任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private Optional<DocumentJob> recordSuccessInTransaction(DocumentPageTask task) {
        DocumentJob document = document(task.documentId());
        if (document.status() == DocumentStatus.COMPLETED) {
            skipCompletedDocument(task);
            return Optional.empty();
        } else if (document.stage() == ProcessingStage.MERGE_TEXT || document.stage() == ProcessingStage.SAVE_TEXT) {
            skipAggregatingDocument(task, document.stage());
            return Optional.empty();
        } else {
            // 未完成文档继续刷新页进度，并在最后一页完成时生成最终 OCR 结果。
        }
        int completedPages = completedPages(task.documentId());
        documentRepository.update(document.markPageCompleted(completedPages, document.totalPages(),
                OffsetDateTime.now()));
        if (completedPages == document.totalPages()) {
            return Optional.of(document);
        } else {
            // 页任务尚未全部完成时只更新进度，不生成最终文档结果。
            return Optional.empty();
        }
    }

    /**
     * 跳过已完成文档的重复成功回调。
     *
     * @param task 已完成页任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void skipCompletedDocument(DocumentPageTask task) {
        LOGGER.info("[页任务聚合] 文档已完成，跳过重复页成功回调 documentId={}, pageNo={}", task.documentId(),
                task.pageNo());
    }

    /**
     * 跳过已经进入后处理阶段的重复成功回调。
     *
     * @param task 已完成页任务
     * @param stage 当前文档阶段
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private void skipAggregatingDocument(DocumentPageTask task, ProcessingStage stage) {
        LOGGER.info("[页任务聚合] 文档已进入聚合后处理阶段，跳过重复页成功回调 documentId={}, pageNo={}, stage={}",
                task.documentId(), task.pageNo(), stage);
    }

    /**
     * 查询文档任务。
     *
     * @param documentId 文档 ID
     * @return 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentJob document(String documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalStateException("document not found: " + documentId));
    }

    /**
     * 统计已完成页任务数。
     *
     * @param documentId 文档 ID
     * @return 已完成页任务数
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private int completedPages(String documentId) {
        long count = pageTaskRepository.listByDocumentId(documentId).stream()
                .filter(task -> task.status() == DocumentPageTaskStatus.COMPLETED)
                .count();
        return Math.toIntExact(count);
    }

    /**
     * 完成文档聚合。
     *
     * @param document 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void completeDocument(DocumentJob document) {
        List<DocumentPageResult> pages = sortedPages(document.documentId());
        DocumentJob merging = advanceDocumentStage(document, ProcessingStage.MERGE_TEXT, pages);
        DocumentJob saving = advanceDocumentStage(merging, ProcessingStage.SAVE_TEXT, pages);
        OcrResult result = resultBuilder.build(saving, pages);
        transactionRunner.requiredVoid(() -> saveCompletedDocument(saving, pages, result));
    }

    /**
     * 在短事务内推进文档阶段，避免慢 LLM 调用期间页面仍显示 OCR 中。
     *
     * @param document 文档任务
     * @param stage 下一阶段
     * @param pages 页结果集合
     * @return 推进阶段后的文档任务
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private DocumentJob advanceDocumentStage(
            DocumentJob document,
            ProcessingStage stage,
            List<DocumentPageResult> pages
    ) {
        DocumentJob progressed = document.advanceStage(stage, pages.size(), document.totalPages(),
                OffsetDateTime.now());
        transactionRunner.requiredVoid(() -> documentRepository.update(progressed));
        return progressed;
    }

    /**
     * 在短事务内保存文档最终结果。
     *
     * @param document 文档任务
     * @param pages 页结果集合
     * @param result OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private void saveCompletedDocument(DocumentJob document, List<DocumentPageResult> pages, OcrResult result) {
        resultRepository.save(result);
        DocumentJob completed = document.advanceStage(ProcessingStage.SAVE_TEXT, pages.size(),
                document.totalPages(), OffsetDateTime.now()).complete(result.resultId(), OffsetDateTime.now());
        documentRepository.update(completed);
        completionCallbackRecorder.record(completed, result);
        refreshBatchSummary(completed.batchId());
    }

    /**
     * 刷新批次摘要，保持公开批次 API 与页任务异步聚合后的文档状态一致。
     *
     * @param batchId 批次 ID
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void refreshBatchSummary(String batchId) {
        if (batchRepository.findById(batchId).isPresent()) {
            updateExistingBatchSummary(batchId);
        } else {
            LOGGER.warn("[页任务聚合] 批次不存在，跳过摘要刷新 batchId={}", batchId);
        }
    }

    /**
     * 更新已存在批次的完成摘要。
     *
     * @param batchId 批次 ID
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void updateExistingBatchSummary(String batchId) {
        List<DocumentJob> documents = documentRepository.listByBatchId(batchId);
        long completed = documents.stream().filter(document -> document.status() == DocumentStatus.COMPLETED).count();
        long failed = documents.stream().filter(document -> document.status().isFailureLike()).count();
        batchRepository.updateSummary(batchId, Math.toIntExact(completed), Math.toIntExact(failed),
                resolveBatchStatus(documents.size(), completed, failed));
    }

    /**
     * 根据文档终态数量解析批次状态。
     *
     * @param total 文档总数
     * @param completed 完成数量
     * @param failed 失败数量
     * @return 批次状态
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private BatchStatus resolveBatchStatus(int total, long completed, long failed) {
        if (completed + failed < total) {
            // 存在未完成的异步页任务时，批次仍处于处理中。
            return BatchStatus.PROCESSING;
        } else if (failed == 0 && completed == total) {
            // 全部文档成功完成时，批次完成。
            return BatchStatus.COMPLETED;
        } else if (completed == 0 && failed == total) {
            // 全部文档失败时，批次失败。
            return BatchStatus.FAILED;
        } else {
            // 成功和失败文档同时存在时，批次部分失败。
            return BatchStatus.PARTIAL_FAILED;
        }
    }

    /**
     * 查询并按页码排序页结果。
     *
     * @param documentId 文档 ID
     * @return 已排序页结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private List<DocumentPageResult> sortedPages(String documentId) {
        return pageResultRepository.listByDocumentId(documentId).stream()
                .sorted(Comparator.comparingInt(DocumentPageResult::pageNo))
                .toList();
    }

}
