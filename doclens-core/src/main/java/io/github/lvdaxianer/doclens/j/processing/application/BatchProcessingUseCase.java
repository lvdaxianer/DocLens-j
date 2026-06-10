package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.DefaultAdapterRegistry;
import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.EventCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentProgressReporter;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionRequest;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionResult;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractor;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 按文档顺序处理单个批次的用例。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class BatchProcessingUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchProcessingUseCase.class);
    private static final int RESULT_SUMMARY_CAPACITY = 5;
    private static final String PAGE_COUNT_FIELD = "pageCount";
    private static final String BLOCK_COUNT_FIELD = "blockCount";
    private static final String TABLE_COUNT_FIELD = "tableCount";
    private static final String CONFIDENCE_FIELD = "confidence";
    private static final String CALLBACK_BODY_FIELD = "callback_body";
    private static final String OCR_TEXT_FIELD = "ocr_text";
    private static final String LLM_MARKDOWN_APPLIED_FIELD = "llm_markdown_applied";
    private static final String LLM_ERROR_MESSAGE_FIELD = "llm_error_message";
    private static final String LLM_MARKDOWN_WARNING = "llm_markdown_post_processing_failed";
    private static final String UNKNOWN_ERROR_TYPE = "unknown";
    private static final int LLM_MARKDOWN_MAX_ATTEMPTS = 3;
    private static final int RAW_OUTPUT_TRACE_CAPACITY = 3;

    private final DocumentJobRepository documentRepository;
    private final OcrResultRepository resultRepository;
    private final OcrEventRepository eventRepository;
    private final BatchRepository batchRepository;
    private final DefaultAdapterRegistry adapterRegistry;
    private final ObjectStorage objectStorage;
    private final DocumentTextExtractor documentTextExtractor;
    private final IdGenerator idGenerator;
    private final OcrEventFactory eventFactory;
    private final MarkdownPostProcessor markdownPostProcessor;
    private final TransactionRunner transactionRunner;

    /**
     * 创建批次处理用例。
     *
     * @param dependencies 用例依赖
     * @param transactionRunner 事务执行器
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public BatchProcessingUseCase(BatchProcessingDependencies dependencies, TransactionRunner transactionRunner) {
        this.documentRepository = dependencies.documentRepository();
        this.resultRepository = dependencies.resultRepository();
        this.eventRepository = dependencies.eventRepository();
        this.batchRepository = dependencies.batchRepository();
        this.adapterRegistry = dependencies.adapterRegistry();
        this.objectStorage = dependencies.objectStorage();
        this.documentTextExtractor = dependencies.documentTextExtractor();
        this.idGenerator = dependencies.idGenerator();
        this.eventFactory = dependencies.eventFactory();
        this.markdownPostProcessor = dependencies.markdownPostProcessor();
        this.transactionRunner = transactionRunner;
    }

    /**
     * 处理批次中的每个文档。
     *
     * @param batchId 批次 ID
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public void processBatch(String batchId) {
        List<DocumentJob> documents = queuedDocuments(batchId);
        Optional<Batch> batch = batchRepository.findById(batchId);
        documents.stream()
                .map(document -> processDocument(document, batch))
                .forEach(result -> transactionRunner.requiredVoid(() -> persistDocumentProcessing(result)));
        transactionRunner.requiredVoid(() -> finishBatch(batchId));
    }

    /**
     * 处理单个文档并转换为持久化计划。
     *
     * @param document 文档任务
     * @param batch 可选批次
     * @return 文档处理结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private DocumentProcessingResult processDocument(DocumentJob document, Optional<Batch> batch) {
        try {
            DocumentJob started = startDocument(document);
            return completeDocument(started, batch);
        } catch (RuntimeException ex) {
            LOGGER.warn("[OCR处理] 文档处理失败 documentId={}, error={}", document.documentId(), ex.getMessage(), ex);
            return failDocument(document, ex);
        }
    }

    /**
     * 标记文档开始处理。
     *
     * @param document 文档任务
     * @return 开始处理后的文档任务
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private DocumentJob startDocument(DocumentJob document) {
        return document.startProcessing(OffsetDateTime.now());
    }

    /**
     * 完成文档解析并构建结果。
     *
     * @param started 已开始处理的文档
     * @param batch 可选批次
     * @return 文档处理结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private DocumentProcessingResult completeDocument(DocumentJob started, Optional<Batch> batch) {
        OcrResult result = buildResult(started);
        int pageCount = Math.max(DocLensConstants.DEFAULT_PAGE_COUNT, result.pageText().size());
        DocumentJob progressed = started.markPageCompleted(pageCount, pageCount, OffsetDateTime.now());
        DocumentJob saving = progressed.advanceStage(ProcessingStage.SAVE_TEXT, pageCount, pageCount,
                OffsetDateTime.now());
        DocumentJob completed = saving.complete(result.resultId(), OffsetDateTime.now());
        CompletedDocumentEventContext eventContext = new CompletedDocumentEventContext(started, progressed, saving,
                completed, result, batch);
        return new DocumentProcessingResult(completed, Optional.of(result),
                completionEvents(eventContext));
    }

    private OcrResult buildResult(DocumentJob document) {
        adapterRegistry.find(document.adapterName())
                .orElseThrow(() -> new IllegalArgumentException("adapter not found: " + document.adapterName()));
        byte[] content = objectStorage.readBytes(document.storageUri());
        DocumentTextExtractionResult extracted = documentTextExtractor.extract(
                new DocumentTextExtractionRequest(document, content, document.adapterName(),
                        stageReporter(document)));
        stageReporter(document).report(ProcessingStage.MERGE_TEXT, extracted.pageText().size(),
                extracted.pageText().size());
        stageReporter(document).report(ProcessingStage.SAVE_TEXT, extracted.pageText().size(),
                extracted.pageText().size());
        PostProcessedText postProcessed = postProcessMarkdown(document, extracted);
        String markdownStorageUri = writeMarkdownResult(document, postProcessed.finalText());
        String resultId = idGenerator.newResultId();
        return new OcrResult(resultId, document.documentId(), postProcessed.finalText(), markdownStorageUri,
                rawOutputWithOcrText(extracted, postProcessed), extracted.structuredDocument(), extracted.pageText(),
                extracted.layoutBlocks(), extracted.tables(), extracted.images(), extracted.confidence(),
                postProcessed.warnings(), OffsetDateTime.now());
    }

    /**
     * 执行可选 Markdown 后处理。
     *
     * @param document 文档任务
     * @param extracted 文本提取结果
     * @return 后处理后的文本
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private PostProcessedText postProcessMarkdown(DocumentJob document, DocumentTextExtractionResult extracted) {
        RuntimeException lastFailure = null;
        MarkdownPostProcessingRequest request = markdownRequest(document, extracted);
        for (int attempt = 1; attempt <= LLM_MARKDOWN_MAX_ATTEMPTS; attempt++) {
            try {
                MarkdownPostProcessingResult result = markdownPostProcessor.process(request);
                return successPostProcessedText(extracted, result);
            } catch (RuntimeException ex) {
                lastFailure = ex;
                // 还没到重试上限时继续重试，避免一次瞬时失败直接回退 OCR。
                if (attempt < LLM_MARKDOWN_MAX_ATTEMPTS) {
                    logMarkdownRetry(document, attempt, ex);
                }
            }
        }
        // 所有尝试都失败后回退 OCR 原文，并保留最后一次失败原因供查询展示。
        return fallbackPostProcessedText(extracted, document, lastFailure);
    }

    /**
     * 构建 Markdown 后处理成功结果。
     *
     * @param extracted 文本提取结果
     * @param result Markdown 后处理结果
     * @return 后处理成功后的文本
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private PostProcessedText successPostProcessedText(
            DocumentTextExtractionResult extracted,
            MarkdownPostProcessingResult result
    ) {
        return new PostProcessedText(result.markdown(), mergeWarnings(extracted.warnings(), result.warnings()),
                result.markdownApplied(), Optional.empty());
    }

    /**
     * 记录 Markdown 后处理失败后的重试日志。
     *
     * @param document 文档任务
     * @param attempt 当前尝试次数
     * @param ex 失败异常
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void logMarkdownRetry(DocumentJob document, int attempt, RuntimeException ex) {
        LOGGER.warn("[LLM后处理] Markdown 后处理失败，准备重试 documentId={}, attempt={}, errorType={}",
                document.documentId(), attempt, ex.getClass().getSimpleName());
    }

    /**
     * 构建 Markdown 后处理耗尽重试后的回退结果。
     *
     * @param extracted 文本提取结果
     * @param document 文档任务
     * @param lastFailure 最后一次失败异常
     * @return 回退到 OCR 原文后的文本
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private PostProcessedText fallbackPostProcessedText(
            DocumentTextExtractionResult extracted,
            DocumentJob document,
            RuntimeException lastFailure
    ) {
        LOGGER.warn("[LLM后处理] Markdown 后处理重试耗尽并回退 OCR 原文 documentId={}, attempts={}, errorType={}",
                document.documentId(), LLM_MARKDOWN_MAX_ATTEMPTS,
                lastFailure == null ? UNKNOWN_ERROR_TYPE : lastFailure.getClass().getSimpleName());
        return new PostProcessedText(extracted.finalText(), failedWarnings(extracted.warnings()), false,
                Optional.ofNullable(lastFailure).map(RuntimeException::getMessage).filter(message -> !message.isBlank()));
    }

    /**
     * 构建 Markdown 后处理请求。
     *
     * @param document 文档任务
     * @param extracted 文本提取结果
     * @return Markdown 后处理请求
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private MarkdownPostProcessingRequest markdownRequest(DocumentJob document, DocumentTextExtractionResult extracted) {
        return new MarkdownPostProcessingRequest(document.documentId(), document.fileName(),
                document.metadata().values(), extracted.finalText());
    }

    /**
     * 在原始输出中保留 OCR 合并文本。
     *
     * @param extracted 文本提取结果
     * @return 带 OCR 原文追溯字段的原始输出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private Map<String, Object> rawOutputWithOcrText(DocumentTextExtractionResult extracted, PostProcessedText postProcessed) {
        Map<String, Object> rawOutput = new LinkedHashMap<>(extracted.rawOutput().size() + RAW_OUTPUT_TRACE_CAPACITY);
        rawOutput.putAll(extracted.rawOutput());
        rawOutput.put(OCR_TEXT_FIELD, extracted.finalText());
        rawOutput.put(LLM_MARKDOWN_APPLIED_FIELD, postProcessed.llmMarkdownApplied());
        postProcessed.llmErrorMessage().ifPresent(message -> rawOutput.put(LLM_ERROR_MESSAGE_FIELD, message));
        return rawOutput;
    }

    /**
     * 合并 OCR 与 Markdown 后处理警告。
     *
     * @param ocrWarnings OCR 警告
     * @param markdownWarnings Markdown 后处理警告
     * @return 合并后的警告
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private List<String> mergeWarnings(List<String> ocrWarnings, List<String> markdownWarnings) {
        List<String> warnings = new ArrayList<>(ocrWarnings.size() + markdownWarnings.size());
        warnings.addAll(ocrWarnings);
        warnings.addAll(markdownWarnings);
        return warnings;
    }

    /**
     * 构建 LLM 失败后的警告集合。
     *
     * @param ocrWarnings OCR 警告
     * @return 带失败警告的集合
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private List<String> failedWarnings(List<String> ocrWarnings) {
        List<String> warnings = new ArrayList<>(ocrWarnings.size() + 1);
        warnings.addAll(ocrWarnings);
        warnings.add(LLM_MARKDOWN_WARNING);
        return warnings;
    }

    private String writeMarkdownResult(DocumentJob document, String finalText) {
        String objectKey = "results/%s/%s/%s".formatted(document.batchId(), document.documentId(),
                MarkdownResultNamer.markdownFileName(document.fileName(), UUID.randomUUID()));
        return objectStorage.writeBytes(objectKey, finalText.getBytes(StandardCharsets.UTF_8));
    }

    private DocumentProcessingResult failDocument(DocumentJob document, RuntimeException ex) {
        DocumentJob failed = latestDocument(document).fail(DocLensConstants.ERROR_CODE_OCR_FAILED, ex.getMessage(),
                OffsetDateTime.now());
        OcrEvent event = event(new DocumentEventPlan(failed, DocLensConstants.EVENT_DOCUMENT_FAILED,
                Map.of("percent", DocLensConstants.COMPLETED_PROGRESS_PERCENT),
                Map.of("code", DocLensConstants.ERROR_CODE_OCR_FAILED, "message", ex.getMessage())));
        return new DocumentProcessingResult(failed, Optional.empty(), List.of(event));
    }

    private DocumentJob latestDocument(DocumentJob document) {
        return documentRepository.findById(document.documentId()).orElse(document);
    }

    /**
     * 构建文档完成链路事件集合。
     *
     * @param context 文档完成事件上下文
     * @return 文档完成链路事件集合
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private List<OcrEvent> completionEvents(CompletedDocumentEventContext context) {
        return List.of(
                event(new DocumentEventPlan(context.started(), DocLensConstants.EVENT_DOCUMENT_STARTED,
                        Map.of("percent", DocLensConstants.START_PROGRESS_PERCENT), Map.of())),
                event(new DocumentEventPlan(context.progressed(), DocLensConstants.EVENT_DOCUMENT_PAGE_COMPLETED,
                        pageProgress(context.progressed()), Map.of())),
                event(new DocumentEventPlan(context.saving(), DocLensConstants.EVENT_DOCUMENT_STAGE_CHANGED,
                        pageProgress(context.saving()), Map.of("stage", context.saving().stage().name().toLowerCase()))),
                event(new DocumentEventPlan(context.completed(), DocLensConstants.EVENT_DOCUMENT_COMPLETED,
                        Map.of("percent", DocLensConstants.COMPLETED_PROGRESS_PERCENT),
                        resultSummary(context.completed(), context.result(), context.batch())))
        );
    }

    private DocumentProgressReporter stageReporter(DocumentJob document) {
        return (stage, completedImages, totalImages) -> transactionRunner.requiredVoid(() ->
                documentRepository.update(progressDocument(document.documentId(), stage, completedImages, totalImages)));
    }

    private DocumentJob progressDocument(
            String documentId,
            ProcessingStage stage,
            int completedImages,
            int totalImages
    ) {
        DocumentJob current = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalStateException("document not found: " + documentId));
        return current.advanceStage(stage, completedImages, totalImages, OffsetDateTime.now());
    }

    private Map<String, Object> pageProgress(DocumentJob document) {
        return Map.of("percent", document.progressPercent(), "current_page", document.currentPage(),
                "total_pages", document.totalPages());
    }

    /**
     * 构建文档完成事件的结果摘要。
     *
     * @param document 文档任务
     * @param result OCR 结果
     * @param batch 可选批次
     * @return 结果摘要
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private Map<String, Object> resultSummary(DocumentJob document, OcrResult result, Optional<Batch> batch) {
        Map<String, Object> summary = new LinkedHashMap<>(RESULT_SUMMARY_CAPACITY);
        summary.put(PAGE_COUNT_FIELD, result.pageText().size());
        summary.put(BLOCK_COUNT_FIELD, result.layoutBlocks().size());
        summary.put(TABLE_COUNT_FIELD, result.tables().size());
        summary.put(CONFIDENCE_FIELD, result.confidence());
        summary.put(CALLBACK_BODY_FIELD, callbackBody(document, result, batch));
        return summary;
    }

    /**
     * 构建解析完成后的回调 body。
     *
     * @param document 文档任务
     * @param result OCR 结果
     * @param batch 可选批次
     * @return 回调 body
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private Map<String, Object> callbackBody(DocumentJob document, OcrResult result, Optional<Batch> batch) {
        if (batch.isPresent()) {
            // 批次存在时使用上传时保存的幂等键构造回调契约。
            return DocumentCompletedCallbackBody.from(batch.get(), document, result).toMap();
        } else {
            // 批次缺失时仍返回稳定契约，避免回调消费方收到不完整结构。
            return new DocumentCompletedCallbackBody(document.metadata().values(), result.finalText(), "").toMap();
        }
    }

    /**
     * 文档完成事件构建上下文。
     *
     * @param started 开始处理阶段文档
     * @param progressed 图片页完成阶段文档
     * @param saving 保存文本阶段文档
     * @param completed 完成阶段文档
     * @param result OCR 结果
     * @param batch 可选批次
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private record CompletedDocumentEventContext(
            DocumentJob started,
            DocumentJob progressed,
            DocumentJob saving,
            DocumentJob completed,
            OcrResult result,
            Optional<Batch> batch
    ) {
    }

    /**
     * Markdown 后处理后的文本和警告。
     *
     * @param finalText 最终文本
     * @param warnings 警告集合
     * @param llmMarkdownApplied 是否应用了 LLM Markdown
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private record PostProcessedText(
            String finalText,
            List<String> warnings,
            boolean llmMarkdownApplied,
            Optional<String> llmErrorMessage
    ) {
    }

    /**
     * 读取批次内仍需执行的排队文档。
     *
     * @param batchId 批次 ID
     * @return 待处理文档集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private List<DocumentJob> queuedDocuments(String batchId) {
        return documentRepository.listByBatchId(batchId).stream()
                .filter(document -> document.status() == DocumentStatus.QUEUED)
                .toList();
    }

    void persistDocumentProcessing(DocumentProcessingResult result) {
        documentRepository.updateAll(List.of(result.document()));
        resultRepository.saveAll(result.result().stream().toList());
        eventRepository.saveAll(result.events());
    }

    private void finishBatch(String batchId) {
        List<DocumentJob> documents = documentRepository.listByBatchId(batchId);
        long completed = documents.stream().filter(document -> document.status() == DocumentStatus.COMPLETED).count();
        long failed = documents.stream().filter(document -> document.status().isFailureLike()).count();
        BatchStatus status = resolveBatchStatus(documents.size(), completed, failed);
        batchRepository.updateSummary(batchId, Math.toIntExact(completed), Math.toIntExact(failed), status);
        if (!documents.isEmpty()) {
            DocumentJob first = documents.getFirst();
            eventRepository.save(batchFinishedEvent(batchId, status, first));
        } else {
            // 接收入库用例不会创建空批次。
        }
    }

    private OcrEvent batchFinishedEvent(String batchId, BatchStatus status, DocumentJob first) {
        return eventFactory.create(new EventCreateRequest(batchId, Optional.empty(),
                "batch." + status.name().toLowerCase(), status.name().toLowerCase(), status.name().toLowerCase(),
                Map.of("percent", DocLensConstants.COMPLETED_PROGRESS_PERCENT), first.metadata(), Optional.empty(),
                Map.of(), Map.of()));
    }

    private BatchStatus resolveBatchStatus(int total, long completed, long failed) {
        if (failed == 0 && completed == total) {
            return BatchStatus.COMPLETED;
        } else if (completed == 0 && failed == total) {
            return BatchStatus.FAILED;
        } else {
            return BatchStatus.PARTIAL_FAILED;
        }
    }

    private OcrEvent event(DocumentEventPlan plan) {
        DocumentJob document = plan.document();
        return eventFactory.create(new EventCreateRequest(document.batchId(), Optional.of(document.documentId()),
                plan.eventType(), document.status().name().toLowerCase(), document.stage().name().toLowerCase(),
                plan.progress(), document.metadata(), document.resultId(), summaryDetail(plan), errorDetail(plan)));
    }

    private Map<String, Object> summaryDetail(DocumentEventPlan plan) {
        if (!DocLensConstants.EVENT_DOCUMENT_FAILED.equals(plan.eventType())) {
            return plan.detail();
        } else {
            return Map.of();
        }
    }

    private Map<String, Object> errorDetail(DocumentEventPlan plan) {
        if (DocLensConstants.EVENT_DOCUMENT_FAILED.equals(plan.eventType())) {
            return plan.detail();
        } else {
            return Map.of();
        }
    }
}
