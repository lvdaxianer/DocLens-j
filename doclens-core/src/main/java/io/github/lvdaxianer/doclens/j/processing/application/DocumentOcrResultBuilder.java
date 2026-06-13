package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.DefaultAdapterRegistry;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentProgressReporter;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionRequest;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionResult;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractor;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文档 OCR 结果构建器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class DocumentOcrResultBuilder {

    private static final String OCR_TEXT_FIELD = "ocr_text";
    private static final String LLM_MARKDOWN_APPLIED_FIELD = "llm_markdown_applied";
    private static final String LLM_ERROR_MESSAGE_FIELD = "llm_error_message";
    private static final int RAW_OUTPUT_TRACE_BASE_CAPACITY = 3;

    private final DocumentOcrResultBuilderDependencies dependencies;
    private final DocumentMarkdownPostProcessingService markdownPostProcessingService;

    /**
     * 创建文档 OCR 结果构建器。
     *
     * @param dependencies 构建器依赖
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    DocumentOcrResultBuilder(DocumentOcrResultBuilderDependencies dependencies) {
        this.dependencies = dependencies;
        this.markdownPostProcessingService = new DocumentMarkdownPostProcessingService(
                dependencies.markdownPostProcessor());
    }

    /**
     * 构建文档 OCR 结果。
     *
     * @param document 文档任务
     * @return OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    OcrResult build(DocumentJob document) {
        ensureAdapterExists(document);
        DocumentTextExtractionResult extracted = extractText(document);
        reportMergeAndSaveStages(document, extracted);
        DocumentPostProcessedText postProcessed = markdownPostProcessingService.process(document, extracted);
        String markdownStorageUri = writeMarkdownResult(document, postProcessed.finalText());
        return buildOcrResult(document, extracted, postProcessed, markdownStorageUri);
    }

    /**
     * 校验文档适配器存在。
     *
     * @param document 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void ensureAdapterExists(DocumentJob document) {
        dependencies.adapterRegistry().find(document.adapterName())
                .orElseThrow(() -> new IllegalArgumentException("adapter not found: " + document.adapterName()));
    }

    /**
     * 提取文档文本。
     *
     * @param document 文档任务
     * @return 文本提取结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentTextExtractionResult extractText(DocumentJob document) {
        byte[] content = dependencies.objectStorage().readBytes(document.storageUri());
        return dependencies.documentTextExtractor().extract(new DocumentTextExtractionRequest(document, content,
                document.adapterName(), stageReporter(document)));
    }

    /**
     * 报告合并文本与保存文本阶段进度。
     *
     * @param document 文档任务
     * @param extracted 文本提取结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void reportMergeAndSaveStages(DocumentJob document, DocumentTextExtractionResult extracted) {
        stageReporter(document).report(ProcessingStage.MERGE_TEXT, extracted.pageText().size(),
                extracted.pageText().size());
        stageReporter(document).report(ProcessingStage.SAVE_TEXT, extracted.pageText().size(),
                extracted.pageText().size());
    }

    /**
     * 创建 OCR 结果聚合。
     *
     * @param document 文档任务
     * @param extracted 文本提取结果
     * @param postProcessed Markdown 后处理结果
     * @param markdownStorageUri Markdown 存储地址
     * @return OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private OcrResult buildOcrResult(
            DocumentJob document,
            DocumentTextExtractionResult extracted,
            DocumentPostProcessedText postProcessed,
            String markdownStorageUri
    ) {
        String resultId = dependencies.idGenerator().newResultId();
        return new OcrResult(resultId, document.documentId(), postProcessed.finalText(), markdownStorageUri,
                rawOutputWithOcrText(extracted, postProcessed), extracted.structuredDocument(), extracted.pageText(),
                extracted.layoutBlocks(), extracted.tables(), extracted.images(), extracted.confidence(),
                postProcessed.warnings(), OffsetDateTime.now());
    }

    /**
     * 在原始输出中保留 OCR 合并文本。
     *
     * @param extracted 文本提取结果
     * @param postProcessed Markdown 后处理结果
     * @return 带 OCR 原文追溯字段的原始输出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private Map<String, Object> rawOutputWithOcrText(
            DocumentTextExtractionResult extracted,
            DocumentPostProcessedText postProcessed
    ) {
        Map<String, Object> rawOutput = new LinkedHashMap<>(extracted.rawOutput().size()
                + RAW_OUTPUT_TRACE_BASE_CAPACITY + postProcessed.llmMetadata().size());
        rawOutput.putAll(extracted.rawOutput());
        rawOutput.put(OCR_TEXT_FIELD, extracted.finalText());
        rawOutput.put(LLM_MARKDOWN_APPLIED_FIELD, postProcessed.llmMarkdownApplied());
        rawOutput.putAll(postProcessed.llmMetadata());
        postProcessed.llmErrorMessage().ifPresent(message -> rawOutput.put(LLM_ERROR_MESSAGE_FIELD, message));
        return rawOutput;
    }

    /**
     * 写入 Markdown 结果文件。
     *
     * @param document 文档任务
     * @param finalText 最终文本
     * @return Markdown 存储地址
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private String writeMarkdownResult(DocumentJob document, String finalText) {
        String objectKey = "results/%s/%s/%s".formatted(document.batchId(), document.documentId(),
                MarkdownResultNamer.markdownFileName(document.fileName(), UUID.randomUUID()));
        return dependencies.objectStorage().writeBytes(objectKey, finalText.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 创建阶段进度上报器。
     *
     * @param document 文档任务
     * @return 进度上报器
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentProgressReporter stageReporter(DocumentJob document) {
        return (stage, completedImages, totalImages) -> dependencies.transactionRunner().requiredVoid(() ->
                dependencies.documentRepository().update(progressDocument(document.documentId(), stage,
                        completedImages, totalImages)));
    }

    /**
     * 构建进度更新后的文档。
     *
     * @param documentId 文档 ID
     * @param stage 处理阶段
     * @param completedImages 已完成图片数
     * @param totalImages 总图片数
     * @return 进度更新后的文档
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentJob progressDocument(
            String documentId,
            ProcessingStage stage,
            int completedImages,
            int totalImages
    ) {
        DocumentJob current = dependencies.documentRepository().findById(documentId)
                .orElseThrow(() -> new IllegalStateException("document not found: " + documentId));
        return current.advanceStage(stage, completedImages, totalImages, OffsetDateTime.now());
    }

    /**
     * 文档 OCR 结果构建器依赖。
     *
     * @param adapterRegistry 适配器注册表
     * @param objectStorage 对象存储
     * @param documentTextExtractor 文档文本提取器
     * @param idGenerator ID 生成器
     * @param markdownPostProcessor Markdown 后处理器
     * @param transactionRunner 事务执行器
     * @param documentRepository 文档仓储
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    record DocumentOcrResultBuilderDependencies(
            DefaultAdapterRegistry adapterRegistry,
            ObjectStorage objectStorage,
            DocumentTextExtractor documentTextExtractor,
            IdGenerator idGenerator,
            MarkdownPostProcessor markdownPostProcessor,
            TransactionRunner transactionRunner,
            DocumentJobRepository documentRepository
    ) {
    }
}
