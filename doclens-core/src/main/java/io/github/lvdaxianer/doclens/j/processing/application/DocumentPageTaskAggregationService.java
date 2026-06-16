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
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
    private static final String PAGE_NO_FIELD = "pageNo";
    private static final String TEXT_FIELD = "text";
    private static final String RESULTS_PREFIX = "results/%s/%s/%s";
    private static final int RAW_OUTPUT_CAPACITY = 2;

    private final DocumentJobRepository documentRepository;
    private final BatchRepository batchRepository;
    private final DocumentPageTaskRepository pageTaskRepository;
    private final DocumentPageResultRepository pageResultRepository;
    private final OcrResultRepository resultRepository;
    private final ObjectStorage objectStorage;
    private final IdGenerator idGenerator;
    private final DocumentCompletionCallbackRecorder completionCallbackRecorder;
    private final TransactionRunner transactionRunner;
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
        this.objectStorage = dependencies.objectStorage();
        this.idGenerator = dependencies.idGenerator();
        this.completionCallbackRecorder = new DocumentCompletionCallbackRecorder(dependencies);
        this.transactionRunner = transactionRunner;
    }

    /**
     * 记录单页 OCR 成功并在全部页完成后聚合文档结果。
     *
     * @param task 已完成页任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    public void recordSuccess(DocumentPageTask task) {
        transactionRunner.requiredVoid(() -> recordSuccessInTransaction(task));
    }

    /**
     * 在事务中记录页成功。
     *
     * @param task 已完成页任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void recordSuccessInTransaction(DocumentPageTask task) {
        DocumentJob document = document(task.documentId());
        if (document.status() == DocumentStatus.COMPLETED) {
            skipCompletedDocument(task);
            return;
        } else {
            // 未完成文档继续刷新页进度，并在最后一页完成时生成最终 OCR 结果。
        }
        int completedPages = completedPages(task.documentId());
        documentRepository.update(document.markPageCompleted(completedPages, document.totalPages(),
                OffsetDateTime.now()));
        if (completedPages == document.totalPages()) {
            completeDocument(document);
        } else {
            // 页任务尚未全部完成时只更新进度，不生成最终文档结果。
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
        OcrResult result = ocrResult(document, pages);
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

    /**
     * 构建最终 OCR 结果。
     *
     * @param document 文档任务
     * @param pages 页结果集合
     * @return OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private OcrResult ocrResult(DocumentJob document, List<DocumentPageResult> pages) {
        String finalText = finalText(pages);
        String markdownUri = writeMarkdownResult(document, finalText);
        return new OcrResult(idGenerator.newResultId(), document.documentId(), finalText, markdownUri,
                rawOutput(pages), Map.of(), pageText(pages), layoutBlocks(pages), List.of(), List.of(),
                confidence(pages), warnings(pages), OffsetDateTime.now());
    }

    /**
     * 合并最终文本。
     *
     * @param pages 页结果集合
     * @return 最终文本
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private String finalText(List<DocumentPageResult> pages) {
        return pages.stream().map(DocumentPageResult::pageText).collect(Collectors.joining("\n\n"));
    }

    /**
     * 写入 Markdown 结果。
     *
     * @param document 文档任务
     * @param finalText 最终文本
     * @return Markdown 存储地址
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private String writeMarkdownResult(DocumentJob document, String finalText) {
        String fileName = MarkdownResultNamer.markdownFileName(document.fileName(), UUID.randomUUID());
        String objectKey = RESULTS_PREFIX.formatted(document.batchId(), document.documentId(), fileName);
        return objectStorage.writeBytes(objectKey, finalText.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 构建原始输出。
     *
     * @param pages 页结果集合
     * @return 原始输出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private Map<String, Object> rawOutput(List<DocumentPageResult> pages) {
        Map<String, Object> rawOutput = new LinkedHashMap<>(RAW_OUTPUT_CAPACITY);
        rawOutput.put("pages", pages.stream().map(DocumentPageResult::rawOutput).toList());
        rawOutput.put("ocr_text", finalText(pages));
        return rawOutput;
    }

    /**
     * 构建页面文本结构。
     *
     * @param pages 页结果集合
     * @return 页面文本结构
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private List<Map<String, Object>> pageText(List<DocumentPageResult> pages) {
        return pages.stream().map(page -> Map.<String, Object>of(PAGE_NO_FIELD, page.pageNo(),
                TEXT_FIELD, page.pageText())).toList();
    }

    /**
     * 合并版面块。
     *
     * @param pages 页结果集合
     * @return 版面块集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private List<Map<String, Object>> layoutBlocks(List<DocumentPageResult> pages) {
        return pages.stream().flatMap(page -> page.layoutBlocks().stream()).toList();
    }

    /**
     * 计算平均置信度。
     *
     * @param pages 页结果集合
     * @return 平均置信度
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private double confidence(List<DocumentPageResult> pages) {
        return pages.stream().mapToDouble(DocumentPageResult::confidence).average().orElse(0D);
    }

    /**
     * 合并警告。
     *
     * @param pages 页结果集合
     * @return 警告集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private List<String> warnings(List<DocumentPageResult> pages) {
        return pages.stream().flatMap(page -> page.warnings().stream()).toList();
    }
}
