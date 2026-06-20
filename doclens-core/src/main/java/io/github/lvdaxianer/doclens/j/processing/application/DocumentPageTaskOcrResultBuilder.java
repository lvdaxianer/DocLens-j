package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionResult;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResult;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 页任务 OCR 最终结果构建器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
class DocumentPageTaskOcrResultBuilder {

    private static final String PAGE_NO_FIELD = "pageNo";
    private static final String TEXT_FIELD = "text";
    private static final String RESULTS_PREFIX = "results/%s/%s/%s";
    private static final String OCR_TEXT_FIELD = "ocr_text";
    private static final String LLM_MARKDOWN_APPLIED_FIELD = "llm_markdown_applied";
    private static final String LLM_ERROR_MESSAGE_FIELD = "llm_error_message";
    private static final int RAW_OUTPUT_CAPACITY = 4;

    private final ObjectStorage objectStorage;
    private final IdGenerator idGenerator;
    private final DocumentMarkdownPostProcessingService markdownPostProcessingService;

    /**
     * 创建页任务 OCR 最终结果构建器。
     *
     * @param dependencies 页任务聚合依赖
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    DocumentPageTaskOcrResultBuilder(DocumentPageTaskAggregationDependencies dependencies) {
        this.objectStorage = dependencies.objectStorage();
        this.idGenerator = dependencies.idGenerator();
        this.markdownPostProcessingService = new DocumentMarkdownPostProcessingService(
                dependencies.markdownPostProcessor());
    }

    /**
     * 构建最终 OCR 结果。
     *
     * @param document 文档任务
     * @param pages 页结果集合
     * @return OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    OcrResult build(DocumentJob document, List<DocumentPageResult> pages) {
        DocumentTextExtractionResult extracted = extractedText(document, pages);
        DocumentPostProcessedText postProcessed = markdownPostProcessingService.process(document, extracted);
        String markdownUri = writeMarkdownResult(document, postProcessed.finalText());
        return result(document, extracted, postProcessed, markdownUri, pages);
    }

    /**
     * 创建 OCR 结果聚合。
     *
     * @param document 文档任务
     * @param extracted OCR 文本提取结果
     * @param postProcessed LLM 后处理结果
     * @param markdownUri Markdown 存储地址
     * @param pages 页结果集合
     * @return OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private OcrResult result(DocumentJob document, DocumentTextExtractionResult extracted,
            DocumentPostProcessedText postProcessed, String markdownUri, List<DocumentPageResult> pages) {
        return new OcrResult(idGenerator.newResultId(), document.documentId(), postProcessed.finalText(), markdownUri,
                rawOutput(pages, extracted, postProcessed), extracted.structuredDocument(), extracted.pageText(),
                extracted.layoutBlocks(), extracted.tables(), extracted.images(), extracted.confidence(),
                postProcessed.warnings(), OffsetDateTime.now());
    }

    /**
     * 将页结果转换为文档文本提取结果。
     *
     * @param document 文档任务
     * @param pages 页结果集合
     * @return 文档文本提取结果
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private DocumentTextExtractionResult extractedText(DocumentJob document, List<DocumentPageResult> pages) {
        return new DocumentTextExtractionResult(finalText(pages), rawOutputWithoutLlm(pages),
                structuredDocument(document, pages), pageText(pages), layoutBlocks(pages), List.of(), List.of(),
                confidence(pages), warnings(pages));
    }

    /**
     * 写入 Markdown 结果。
     *
     * @param document 文档任务
     * @param finalText 最终文本
     * @return Markdown 存储地址
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private String writeMarkdownResult(DocumentJob document, String finalText) {
        String fileName = MarkdownResultNamer.markdownFileName(document.fileName(), UUID.randomUUID());
        String objectKey = RESULTS_PREFIX.formatted(document.batchId(), document.documentId(), fileName);
        return objectStorage.writeBytes(objectKey, finalText.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 构建带 LLM 观测字段的原始输出。
     *
     * @param pages 页结果集合
     * @param extracted OCR 文本提取结果
     * @param postProcessed LLM 后处理结果
     * @return 原始输出
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private Map<String, Object> rawOutput(
            List<DocumentPageResult> pages,
            DocumentTextExtractionResult extracted,
            DocumentPostProcessedText postProcessed
    ) {
        Map<String, Object> rawOutput = rawOutputWithoutLlm(pages);
        rawOutput.put(OCR_TEXT_FIELD, extracted.finalText());
        rawOutput.put(LLM_MARKDOWN_APPLIED_FIELD, postProcessed.llmMarkdownApplied());
        rawOutput.putAll(postProcessed.llmMetadata());
        postProcessed.llmErrorMessage().ifPresent(message -> rawOutput.put(LLM_ERROR_MESSAGE_FIELD, message));
        return rawOutput;
    }

    /**
     * 构建不含 LLM 字段的原始 OCR 输出。
     *
     * @param pages 页结果集合
     * @return OCR 原始输出
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private Map<String, Object> rawOutputWithoutLlm(List<DocumentPageResult> pages) {
        Map<String, Object> rawOutput = new LinkedHashMap<>(RAW_OUTPUT_CAPACITY);
        rawOutput.put("pages", pages.stream().map(DocumentPageResult::rawOutput).toList());
        rawOutput.put(OCR_TEXT_FIELD, finalText(pages));
        return rawOutput;
    }

    /**
     * 构建结构化文档视图。
     *
     * @param document 文档任务
     * @param pages 页结果集合
     * @return 结构化文档
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private Map<String, Object> structuredDocument(DocumentJob document, List<DocumentPageResult> pages) {
        return Map.of("documentId", document.documentId(), "fileName", document.fileName(), "pages",
                pageText(pages));
    }

    /**
     * 构建页面文本结构。
     *
     * @param pages 页结果集合
     * @return 页面文本结构
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private List<Map<String, Object>> pageText(List<DocumentPageResult> pages) {
        return pages.stream().map(page -> Map.<String, Object>of(PAGE_NO_FIELD, page.pageNo(),
                TEXT_FIELD, page.pageText())).toList();
    }

    /**
     * 合并最终文本。
     *
     * @param pages 页结果集合
     * @return 最终文本
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private String finalText(List<DocumentPageResult> pages) {
        return pages.stream().map(DocumentPageResult::pageText).collect(Collectors.joining("\n\n"));
    }

    /**
     * 合并版面块。
     *
     * @param pages 页结果集合
     * @return 版面块集合
     * @author lvdaxianerplus
     * @date 2026-06-20
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
     * @date 2026-06-20
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
     * @date 2026-06-20
     */
    private List<String> warnings(List<DocumentPageResult> pages) {
        return pages.stream().flatMap(page -> page.warnings().stream()).toList();
    }
}
