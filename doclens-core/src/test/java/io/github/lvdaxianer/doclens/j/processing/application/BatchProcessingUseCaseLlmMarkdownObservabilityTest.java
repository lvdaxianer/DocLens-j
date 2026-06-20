package io.github.lvdaxianer.doclens.j.processing.application;

import static io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCaseMarkdownProcessors.*;
import static io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCaseTestSupport.document;
import static io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCaseTestSupport.useCase;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import org.junit.jupiter.api.Test;

/**
 * 批次处理 LLM Markdown 可观测性测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-19
 */
class BatchProcessingUseCaseLlmMarkdownObservabilityTest {

    /**
     * Markdown 后处理失败时应优先保留根因消息，而不是仅写入外层包装消息。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void processBatchFallsBackToRootCauseMessageWhenLlmFailsWithCause() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        documentRepository.save(document("doc-1", 0));
        BatchProcessingUseCase batchUseCase = useCase(BatchProcessingUseCaseConfig.of(documentRepository,
                resultRepository, new FixedTextExtractor("原始 OCR 文本"),
                new WrappedFailingMarkdownPostProcessor("LLM Markdown request failed", "request timed out")));

        batchUseCase.processBatch("batch-test");

        assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
            assertThat(result.finalText()).isEqualTo("原始 OCR 文本");
            assertThat(result.warnings()).contains("llm_markdown_post_processing_failed");
            assertThat(result.rawVendorOutput())
                    .containsEntry("llm_markdown_applied", false)
                    .containsEntry("llm_error_message", "request timed out");
        });
    }

    /**
     * LLM 第三次重试成功时不应回退 OCR。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void processBatchKeepsMarkdownWhenLlmSucceedsOnThirdAttempt() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        RetryingMarkdownPostProcessor markdownPostProcessor = new RetryingMarkdownPostProcessor(2,
                "# 修复后 Markdown");
        documentRepository.save(document("doc-1", 0));
        BatchProcessingUseCase batchUseCase = useCase(BatchProcessingUseCaseConfig.of(documentRepository,
                resultRepository, new FixedTextExtractor("原始 OCR 文本"), markdownPostProcessor));

        batchUseCase.processBatch("batch-test");

        assertThat(markdownPostProcessor.attempts()).isEqualTo(3);
        assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
            assertThat(result.finalText()).isEqualTo("# 修复后 Markdown");
            assertThat(result.rawVendorOutput())
                    .containsEntry("llm_markdown_applied", true)
                    .doesNotContainKey("llm_error_message");
        });
    }

    /**
     * LLM 连续三次失败后才应回退 OCR。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void processBatchFallsBackAfterThreeLlmFailures() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        CountingFailingMarkdownPostProcessor markdownPostProcessor =
                new CountingFailingMarkdownPostProcessor("llm unavailable");
        documentRepository.save(document("doc-1", 0));
        BatchProcessingUseCase batchUseCase = useCase(BatchProcessingUseCaseConfig.of(documentRepository,
                resultRepository, new FixedTextExtractor("原始 OCR 文本"), markdownPostProcessor));

        batchUseCase.processBatch("batch-test");

        assertThat(markdownPostProcessor.attempts()).isEqualTo(3);
        assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
            assertThat(result.finalText()).isEqualTo("原始 OCR 文本");
            assertThat(result.warnings()).contains("llm_markdown_post_processing_failed");
            assertThat(result.rawVendorOutput())
                    .containsEntry("llm_markdown_applied", false)
                    .containsEntry("llm_error_message", "llm unavailable");
        });
    }

    /**
     * 未配置 LLM 后处理时应明确标记未应用 Markdown 排版。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void processBatchMarksLlmMarkdownAsNotAppliedWhenNoopProcessorIsUsed() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        documentRepository.save(document("doc-1", 0));
        BatchProcessingUseCase batchUseCase = useCase(BatchProcessingUseCaseConfig.of(documentRepository,
                resultRepository, new FixedTextExtractor("原始 OCR 文本"), MarkdownPostProcessor.noop()));

        batchUseCase.processBatch("batch-test");

        assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
            assertThat(result.finalText()).isEqualTo("原始 OCR 文本");
            assertThat(result.rawVendorOutput()).containsEntry("llm_markdown_applied", false);
        });
    }
}
