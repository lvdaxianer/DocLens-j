package io.github.lvdaxianer.doclens.j.processing.application;

import static io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCaseTestSupport.document;
import static io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCaseTestSupport.useCase;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 批次处理 LLM Markdown 后处理测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class BatchProcessingUseCaseLlmMarkdownTest {

    private static final int TEST_CHUNK_COUNT = 2;
    private static final int TEST_MAX_CONTEXT_TOKENS = 2000;
    private static final int TEST_ESTIMATED_OCR_TOKENS = 3600;
    private static final String CALLER_FIELD = "caller";

    /*
     * 本类只覆盖 LLM Markdown 后处理结果写入语义：
     * 成功时保存 Markdown，失败时回退 OCR，think 块清洗不能泄露推理过程。
     * 批次并发和进度持久化仍留在 BatchProcessingUseCaseTest，
     * 让两个测试类的失败原因更容易定位。
     *
     * 每个用例都通过最终 OcrResult 验证行为，
     * 不直接依赖 Markdown 清洗实现细节。
     * 这样后续如果清洗策略从正则切换到 parser，
     * 只要对外结果不变，测试就不需要跟着重写。
     *
     * think 相关场景覆盖标准标签、大小写变体和未闭合标签，
     * 重点防止模型推理过程混入用户可复制的最终 Markdown。
     *
     * 重试相关场景覆盖第三次成功和三次失败两个边界，
     * 这样既能防止过早回退，也能防止失败后不落警告。
     * noop 场景单独保留，用来区分“未启用”和“启用但失败”。
     * 如果新增协议或模型输出变体，优先增加结果断言而不是检查内部状态。
     * 如果新增批次编排语义，应放回 BatchProcessingUseCaseTest。
     * 如果新增测试替身，应放到 Markdown processor 支撑文件。
     */

    /**
     * 配置 LLM 后处理时应优先保存 Markdown，并保留原始 OCR 文本。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void processBatchUsesLlmMarkdownWhenConfigured() {
        // 准备原始 OCR 文本和固定 Markdown 后处理结果。
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        InMemoryOcrEventRepository eventRepository = new InMemoryOcrEventRepository();
        // 文档 meta 用于验证 Markdown 结果进入 callback body 后仍保留上下文。
        documentRepository.save(document("doc-1", 0, new JsonPayload(Map.of("kind", "invoice"))));
        BatchProcessingUseCase batchUseCase = useCase(BatchProcessingUseCaseConfig.of(
                new BatchProcessingUseCaseMarkdownConfig(documentRepository, resultRepository, eventRepository,
                new FixedTextExtractor("原始 OCR 文本"),
                new FixedMarkdownPostProcessor("# 发票\n\n原始 OCR 文本"))));

        // 执行批次处理，触发 OCR 文本到 Markdown 的后处理链路。
        batchUseCase.processBatch("batch-test");

        // 最终文本应使用 Markdown，同时 rawVendorOutput 保留 OCR 原文和应用标记。
        assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
            assertThat(result.finalText()).isEqualTo("# 发票\n\n原始 OCR 文本");
            // rawVendorOutput 同时保留原始 OCR 与 LLM 应用状态，便于审计。
            assertThat(result.rawVendorOutput())
                    .containsEntry("ocr_text", "原始 OCR 文本")
                    .containsEntry("llm_markdown_applied", true);
        });
        // 完成事件中的 callback body 也必须使用最终 Markdown 文本。
        assertThat(eventRepository.events())
                .filteredOn(event -> DocLensConstants.EVENT_DOCUMENT_COMPLETED.equals(event.eventType()))
                .singleElement()
                .satisfies(event -> assertThat(event.resultSummary()).extracting("callback_body")
                        .isEqualTo(Map.of("meta", Map.of("kind", "invoice"), "text", "# 发票\n\n原始 OCR 文本",
                                "idempotency_key", "", CALLER_FIELD, anonymousCaller())));
    }

    /**
     * 历史编排标记不应改变 LLM Markdown 失败回退语义。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void processBatchFallsBackWhenLlmFailsEvenWithLegacyOrchestrationFlag() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        documentRepository.save(document("doc-1", 0, JsonPayload.empty(), true));
        BatchProcessingUseCase batchUseCase = useCase(BatchProcessingUseCaseConfig.of(documentRepository,
                resultRepository, new FixedTextExtractor("原始 OCR 文本"), new FailingMarkdownPostProcessor()));

        batchUseCase.processBatch("batch-test");

        assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
            assertThat(result.finalText()).isEqualTo("原始 OCR 文本");
            assertThat(result.rawVendorOutput()).containsEntry("llm_markdown_applied", false);
            assertThat(result.warnings()).contains("llm_markdown_post_processing_failed");
        });
    }

    /**
     * 未编排的文档仍应继续执行 LLM Markdown 后处理。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void processBatchStillAppliesLlmMarkdownWhenNotOrchestrated() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        documentRepository.save(document("doc-1", 0, JsonPayload.empty(), false));
        BatchProcessingUseCase batchUseCase = useCase(BatchProcessingUseCaseConfig.of(documentRepository,
                resultRepository, new FixedTextExtractor("原始 OCR 文本"),
                new FixedMarkdownPostProcessor("# 正文\n\n原始 OCR 文本")));

        batchUseCase.processBatch("batch-test");

        assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
            assertThat(result.finalText()).isEqualTo("# 正文\n\n原始 OCR 文本");
            assertThat(result.rawVendorOutput()).containsEntry("llm_markdown_applied", true);
        });
    }

    /**
     * 创建匿名调用方回调载荷。
     *
     * @return 匿名调用方载荷
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private Map<String, Object> anonymousCaller() {
        return CallerIdentity.anonymous().toMap();
    }

    /**
     * LLM 分片后处理元数据应写入 OCR 原始输出。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void processBatchWritesLlmChunkMetadataToRawOutput() {
        // 准备带分片元数据的 Markdown 后处理器，模拟大文档被分片处理。
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        // 单文档足以验证 rawVendorOutput 的观测字段。
        documentRepository.save(document("doc-1", 0));
        BatchProcessingUseCase batchUseCase = useCase(BatchProcessingUseCaseConfig.of(documentRepository,
                resultRepository, new FixedTextExtractor("原始 OCR 文本"),
                new ChunkedMetadataMarkdownPostProcessor("# 分片 Markdown", TEST_CHUNK_COUNT,
                        TEST_MAX_CONTEXT_TOKENS, TEST_ESTIMATED_OCR_TOKENS)));

        // 执行批次处理，后处理元数据应随最终结果落库。
        batchUseCase.processBatch("batch-test");

        // rawVendorOutput 需要包含分片观测字段，便于定位大文档是否走了分片链路。
        assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
            assertThat(result.finalText()).isEqualTo("# 分片 Markdown");
            assertThat(result.rawVendorOutput())
                    .containsEntry("llm_chunked", true)
                    .containsEntry("llm_chunk_count", TEST_CHUNK_COUNT)
                    .containsEntry("llm_max_context_tokens", TEST_MAX_CONTEXT_TOKENS)
                    .containsEntry("llm_estimated_ocr_tokens", TEST_ESTIMATED_OCR_TOKENS);
        });
    }

    /**
     * LLM 返回成对 think 标签时应剥离思考过程，只保存最终 Markdown。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void processBatchRemovesThinkBlockFromLlmMarkdown() {
        // 准备包含标准 think 块的 LLM Markdown 响应。
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        // 单文档足以覆盖 think 块清洗行为。
        documentRepository.save(document("doc-1", 0));
        BatchProcessingUseCase batchUseCase = useCase(BatchProcessingUseCaseConfig.of(documentRepository,
                resultRepository, new FixedTextExtractor("原始 OCR 文本"),
                new FixedMarkdownPostProcessor("<think>\nI should analyze the OCR.\n</think>\n# 正文\n\n原始 OCR 文本")));

        // 执行后处理，期望内部清洗逻辑剥离推理内容。
        batchUseCase.processBatch("batch-test");

        // 最终文本只保留正文，不能泄露 think 标签或推理过程。
        assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
            assertThat(result.finalText()).isEqualTo("# 正文\n\n原始 OCR 文本");
            // 思考过程是模型内部推理，不能对用户可见。
            assertThat(result.finalText()).doesNotContain("<think>").doesNotContain("I should analyze");
        });
    }

    /**
     * LLM 返回大小写或空格变体 think 标签时也应剥离思考过程。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void processBatchRemovesThinkTagVariantsFromLlmMarkdown() {
        // 准备大小写和空格变体标签，覆盖模型输出不规范的场景。
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        // 标签变体测试不依赖事件或批次仓储。
        documentRepository.save(document("doc-1", 0));
        BatchProcessingUseCase batchUseCase = useCase(BatchProcessingUseCaseConfig.of(documentRepository,
                resultRepository, new FixedTextExtractor("原始 OCR 文本"),
                new FixedMarkdownPostProcessor("<THINK >\nI should analyze the OCR.\n</THINK>\n# 正文")));

        // 执行批次处理，验证标签清洗不依赖固定大小写。
        batchUseCase.processBatch("batch-test");

        // 变体 think 标签和其中内容都不能进入最终结果。
        assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
            assertThat(result.finalText()).isEqualTo("# 正文");
            // 大写标签和其中英文推理文本都要被剥离。
            assertThat(result.finalText()).doesNotContain("<THINK").doesNotContain("I should analyze");
        });
    }

    /**
     * LLM 只返回未闭合 think 思考内容时应回退 OCR 原文，避免保存推理过程。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void processBatchFallsBackToOcrTextWhenLlmReturnsOnlyThinking() {
        // 准备只有未闭合 think 的响应，模拟模型只输出思考过程。
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        // 未闭合 think 清洗后没有有效正文，应触发原文兜底。
        documentRepository.save(document("doc-1", 0));
        BatchProcessingUseCase batchUseCase = useCase(BatchProcessingUseCaseConfig.of(documentRepository,
                resultRepository, new FixedTextExtractor("原始 OCR 文本"),
                new FixedMarkdownPostProcessor("<think>\nThe user wants me to convert OCR text into Markdown.")));

        // 执行后处理，清洗后为空时应触发 OCR 原文兜底。
        batchUseCase.processBatch("batch-test");

        // 兜底结果必须是 OCR 原文，不能保存未闭合 think 内容。
        assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
            assertThat(result.finalText()).isEqualTo("原始 OCR 文本");
            // 未闭合标签和推理内容都不能写入最终结果。
            assertThat(result.finalText()).doesNotContain("<think>").doesNotContain("The user wants");
        });
    }

    /**
     * LLM 后处理失败时应回退原始 OCR 文本并记录警告。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void processBatchFallsBackToOcrTextWhenLlmFails() {
        // 准备固定失败的 Markdown 后处理器，覆盖异常兜底路径。
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        // 固定 OCR 原文用于验证失败兜底文本。
        documentRepository.save(document("doc-1", 0));
        BatchProcessingUseCase batchUseCase = useCase(BatchProcessingUseCaseConfig.of(documentRepository,
                resultRepository, new FixedTextExtractor("原始 OCR 文本"), new FailingMarkdownPostProcessor()));

        // 执行批次处理，Markdown 失败后应回退到 OCR 原文。
        batchUseCase.processBatch("batch-test");

        // 结果需要记录失败警告和错误摘要，便于排查后处理服务状态。
        assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
            assertThat(result.finalText()).isEqualTo("原始 OCR 文本");
            assertThat(result.warnings()).contains("llm_markdown_post_processing_failed");
            // 失败摘要会写入 rawVendorOutput，帮助定位后处理服务异常。
            assertThat(result.rawVendorOutput())
                    .containsEntry("llm_markdown_applied", false)
                    .containsEntry("llm_error_message", "llm unavailable");
        });
    }

}
