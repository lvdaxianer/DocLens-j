package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionResult;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文档 Markdown 后处理服务。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class DocumentMarkdownPostProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentMarkdownPostProcessingService.class);
    private static final String LLM_MARKDOWN_WARNING = "llm_markdown_post_processing_failed";
    private static final String LLM_THINKING_WARNING = "llm_markdown_thinking_removed";
    private static final String LLM_THINKING_FALLBACK_WARNING = "llm_markdown_thinking_only_fallback";
    private static final String UNKNOWN_ERROR_TYPE = "unknown";
    private static final int LLM_MARKDOWN_MAX_ATTEMPTS = 3;

    private final MarkdownPostProcessor markdownPostProcessor;

    /**
     * 创建文档 Markdown 后处理服务。
     *
     * @param markdownPostProcessor Markdown 后处理器
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    DocumentMarkdownPostProcessingService(MarkdownPostProcessor markdownPostProcessor) {
        this.markdownPostProcessor = markdownPostProcessor;
    }

    /**
     * 执行可选 Markdown 后处理。
     *
     * @param document 文档任务
     * @param extracted 文本提取结果
     * @return 后处理后的文本
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    DocumentPostProcessedText process(DocumentJob document, DocumentTextExtractionResult extracted) {
        RuntimeException lastFailure = null;
        MarkdownPostProcessingRequest request = markdownRequest(document, extracted);
        for (int attempt = 1; attempt <= LLM_MARKDOWN_MAX_ATTEMPTS; attempt++) {
            try {
                MarkdownPostProcessingResult result = markdownPostProcessor.process(request);
                return successPostProcessedText(extracted, result);
            } catch (RuntimeException ex) {
                lastFailure = ex;
                retryIfNeeded(document, attempt, ex);
            }
        }
        return fallbackPostProcessedText(extracted, document, lastFailure);
    }

    /**
     * 记录需要继续执行的 Markdown 后处理重试。
     *
     * @param document 文档任务
     * @param attempt 当前尝试次数
     * @param ex 失败异常
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void retryIfNeeded(DocumentJob document, int attempt, RuntimeException ex) {
        if (attempt < LLM_MARKDOWN_MAX_ATTEMPTS) {
            // 还没到重试上限时继续重试，避免一次瞬时失败直接回退 OCR。
            logMarkdownRetry(document, attempt, ex);
        } else {
            // 达到重试上限时由外层统一执行回退和告警落盘。
        }
    }

    /**
     * 构建 Markdown 后处理成功结果。
     *
     * @param extracted 文本提取结果
     * @param result Markdown 后处理结果
     * @return 后处理成功后的文本
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentPostProcessedText successPostProcessedText(
            DocumentTextExtractionResult extracted,
            MarkdownPostProcessingResult result
    ) {
        MarkdownThinkingSanitizationResult sanitizationResult = MarkdownThinkingSanitizer.sanitize(result.markdown());
        if (sanitizationResult.fallbackToOcrText()) {
            // LLM 只返回思考内容时回退 OCR 原文，避免保存无正文结果。
            return new DocumentPostProcessedText(extracted.finalText(),
                    thinkingFallbackWarnings(extracted.warnings()), false, Optional.empty(), Map.of());
        } else {
            // 正常 Markdown 仅移除思考块，保留 LLM 排版结果。
            return sanitizedPostProcessedText(extracted, result, sanitizationResult);
        }
    }

    /**
     * 构建清洗后的 Markdown 后处理文本。
     *
     * @param extracted 文本提取结果
     * @param result Markdown 后处理结果
     * @param sanitizationResult 思考内容清洗结果
     * @return 后处理文本
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentPostProcessedText sanitizedPostProcessedText(
            DocumentTextExtractionResult extracted,
            MarkdownPostProcessingResult result,
            MarkdownThinkingSanitizationResult sanitizationResult
    ) {
        List<String> warnings = mergeWarnings(extracted.warnings(), thinkingWarnings(result.warnings(),
                result.markdown(), sanitizationResult.markdown()));
        return new DocumentPostProcessedText(sanitizationResult.markdown(), warnings, result.markdownApplied(),
                Optional.empty(), result.metadata());
    }

    /**
     * 构建 Markdown 后处理耗尽重试后的回退结果。
     *
     * @param extracted 文本提取结果
     * @param document 文档任务
     * @param lastFailure 最后一次失败异常
     * @return 回退到 OCR 原文后的文本
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentPostProcessedText fallbackPostProcessedText(
            DocumentTextExtractionResult extracted,
            DocumentJob document,
            RuntimeException lastFailure
    ) {
        LOGGER.warn("[LLM后处理] Markdown 后处理重试耗尽并回退 OCR 原文 documentId={}, attempts={}, errorType={}",
                document.documentId(), LLM_MARKDOWN_MAX_ATTEMPTS, errorType(lastFailure));
        return new DocumentPostProcessedText(extracted.finalText(), failedWarnings(extracted.warnings()), false,
                Optional.ofNullable(lastFailure).map(RuntimeException::getMessage).filter(message -> !message.isBlank()),
                Map.of());
    }

    /**
     * 构建 Markdown 后处理请求。
     *
     * @param document 文档任务
     * @param extracted 文本提取结果
     * @return Markdown 后处理请求
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private MarkdownPostProcessingRequest markdownRequest(DocumentJob document, DocumentTextExtractionResult extracted) {
        return new MarkdownPostProcessingRequest(document.documentId(), document.fileName(),
                document.metadata().values(), extracted.finalText());
    }

    /**
     * 记录 Markdown 后处理失败后的重试日志。
     *
     * @param document 文档任务
     * @param attempt 当前尝试次数
     * @param ex 失败异常
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void logMarkdownRetry(DocumentJob document, int attempt, RuntimeException ex) {
        LOGGER.warn("[LLM后处理] Markdown 后处理失败，准备重试 documentId={}, attempt={}, errorType={}",
                document.documentId(), attempt, ex.getClass().getSimpleName());
    }

    /**
     * 解析异常类型。
     *
     * @param failure 失败异常
     * @return 异常类型
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private String errorType(RuntimeException failure) {
        if (failure == null) {
            // 未捕获到具体异常时使用稳定兜底类型，避免日志出现空值。
            return UNKNOWN_ERROR_TYPE;
        } else {
            // 有具体异常时记录类型，避免日志输出敏感消息正文。
            return failure.getClass().getSimpleName();
        }
    }

    /**
     * 合并 OCR 与 Markdown 后处理警告。
     *
     * @param ocrWarnings OCR 警告
     * @param markdownWarnings Markdown 后处理警告
     * @return 合并后的警告
     * @author lvdaxianerplus
     * @date 2026-06-11
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
     * @date 2026-06-11
     */
    private List<String> failedWarnings(List<String> ocrWarnings) {
        return appendedWarning(ocrWarnings, LLM_MARKDOWN_WARNING);
    }

    /**
     * 构建移除思考过程后的警告集合。
     *
     * @param markdownWarnings Markdown 后处理警告
     * @param originalMarkdown 原始 Markdown
     * @param sanitizedMarkdown 清洗后 Markdown
     * @return 警告集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private List<String> thinkingWarnings(
            List<String> markdownWarnings,
            String originalMarkdown,
            String sanitizedMarkdown
    ) {
        if (originalMarkdown.equals(sanitizedMarkdown)) {
            // 未发生思考内容清洗时保持原警告集合。
            return markdownWarnings;
        } else {
            // 发生清洗时补充可观测警告，方便排查模型输出不稳定。
            return appendedWarning(markdownWarnings, LLM_THINKING_WARNING);
        }
    }

    /**
     * 构建思考内容兜底回退警告集合。
     *
     * @param ocrWarnings OCR 警告
     * @return 警告集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private List<String> thinkingFallbackWarnings(List<String> ocrWarnings) {
        return appendedWarning(ocrWarnings, LLM_THINKING_FALLBACK_WARNING);
    }

    /**
     * 追加单个警告。
     *
     * @param warnings 原警告集合
     * @param warning 新警告
     * @return 追加后的警告集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private List<String> appendedWarning(List<String> warnings, String warning) {
        List<String> mergedWarnings = new ArrayList<>(warnings.size() + 1);
        mergedWarnings.addAll(warnings);
        mergedWarnings.add(warning);
        return mergedWarnings;
    }
}
