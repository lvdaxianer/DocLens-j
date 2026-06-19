package io.github.lvdaxianer.doclens.j.processing.application;

import static io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCaseTestSupport.document;
import static io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCaseTestSupport.useCase;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import org.junit.jupiter.api.Test;

/**
 * 批次处理 LLM Markdown 始终应用测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
class BatchProcessingUseCaseLlmMarkdownAlwaysApplyTest {

    /**
     * 历史编排标记不应跳过已配置的 LLM Markdown 格式化。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void processBatchAppliesLlmMarkdownEvenWhenLegacyOrchestrationFlagIsTrue() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        documentRepository.save(document("doc-1", 0, JsonPayload.empty(), true));
        BatchProcessingUseCase batchUseCase = useCase(BatchProcessingUseCaseConfig.of(documentRepository,
                resultRepository, new FixedTextExtractor("原始 OCR 文本"),
                new FixedMarkdownPostProcessor("# 格式化正文\n\n原始 OCR 文本")));

        batchUseCase.processBatch("batch-test");

        assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
            assertThat(result.finalText()).isEqualTo("# 格式化正文\n\n原始 OCR 文本");
            assertThat(result.rawVendorOutput()).containsEntry("llm_markdown_applied", true);
        });
    }
}
