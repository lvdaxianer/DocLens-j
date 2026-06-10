package io.github.lvdaxianer.doclens.j.processing.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.domain.DefaultAdapterRegistry;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrAdapter;
import io.github.lvdaxianer.doclens.j.api.AdapterCapability;
import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionRequest;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionResult;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractor;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * 批次处理用例测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class BatchProcessingUseCaseTest {

    private static final int TEST_DOCUMENT_CAPACITY = 4;
    private static final int TEST_EVENT_CAPACITY = 8;
    private static final int CONCURRENT_TEST_DOCUMENTS = 2;
    private static final int CONCURRENT_TEST_TIMEOUT_SECONDS = 2;

    /**
     * 每个文档完成后应持久化，避免长批次结束后才统一可见。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void processBatchPersistsEachCompletedDocument() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        documentRepository.saveAll(List.of(document("doc-1", 0), document("doc-2", 1)));
        RecordingDocumentTextExtractor extractor = new RecordingDocumentTextExtractor(documentRepository);
        BatchProcessingUseCase useCase = useCase(documentRepository, extractor);

        useCase.processBatch("batch-test");

        assertThat(documentRepository.findById("doc-1")).get().extracting(DocumentJob::status)
                .isEqualTo(DocumentStatus.COMPLETED);
        assertThat(documentRepository.findById("doc-2")).get().extracting(DocumentJob::status)
                .isEqualTo(DocumentStatus.COMPLETED);
    }

    /**
     * 批次内多个文档应先并发进入提取器，避免第一个文档阻塞时其它 OCR 节点空转。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void processBatchStartsMultipleDocumentsBeforeWaitingForFirstCompletion() throws Exception {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        documentRepository.saveAll(List.of(document("doc-1", 0), document("doc-2", 1)));
        BlockingDocumentTextExtractor extractor = new BlockingDocumentTextExtractor();
        ExecutorService documentExecutor = Executors.newFixedThreadPool(CONCURRENT_TEST_DOCUMENTS);
        BatchProcessingUseCase useCase = useCase(documentRepository, extractor, documentExecutor);
        ExecutorService batchCaller = Executors.newSingleThreadExecutor();

        try {
            batchCaller.submit(() -> useCase.processBatch("batch-test"));

            assertThat(extractor.awaitBothEntered()).isTrue();
            extractor.release();
        } finally {
            extractor.release();
            shutdownExecutor(batchCaller);
            shutdownExecutor(documentExecutor);
        }
    }

    /**
     * 快文档完成后应先持久化，不能被同批次内慢文档阻塞。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void processBatchPersistsFastDocumentBeforeSlowDocumentCompletes() throws Exception {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        documentRepository.saveAll(List.of(document("doc-slow", 0), document("doc-fast", 1)));
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        SlowFirstDocumentTextExtractor extractor = new SlowFirstDocumentTextExtractor();
        ExecutorService documentExecutor = Executors.newFixedThreadPool(CONCURRENT_TEST_DOCUMENTS);
        BatchProcessingUseCase useCase = useCase(new UseCaseTestConfig(documentRepository, resultRepository,
                new InMemoryOcrEventRepository(), new InMemoryBatchRepository(), extractor,
                MarkdownPostProcessor.noop(), documentExecutor));
        ExecutorService batchCaller = Executors.newSingleThreadExecutor();

        try {
            batchCaller.submit(() -> useCase.processBatch("batch-test"));

            assertThat(extractor.awaitFastCompleted()).isTrue();
            assertThat(resultRepository.awaitResult("doc-fast")).isTrue();
            assertThat(resultRepository.findByDocumentId("doc-slow")).isEmpty();
            extractor.releaseSlow();
        } finally {
            extractor.releaseSlow();
            shutdownExecutor(batchCaller);
            shutdownExecutor(documentExecutor);
        }
    }

    /**
     * 提取器上报阶段进度时应立即持久化，便于 Dashboard 展示当前卡点。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void processBatchPersistsStageProgressDuringExtraction() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        documentRepository.save(document("doc-1", 0));
        RecordingDocumentTextExtractor extractor = new RecordingDocumentTextExtractor(documentRepository);
        BatchProcessingUseCase useCase = useCase(documentRepository, extractor);

        useCase.processBatch("batch-test");

        assertThat(extractor.stageWasVisibleDuringExtraction).isTrue();
        assertThat(extractor.currentPageWasVisibleDuringExtraction).isTrue();
        assertThat(documentRepository.findById("doc-1")).get().extracting(DocumentJob::stage)
                .isEqualTo(ProcessingStage.COMPLETED);
    }

    /**
     * 批次处理应跳过非排队文档，避免文档级重试时重跑同批次已完成文档。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void processBatchSkipsDocumentsThatAreNotQueued() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        documentRepository.save(completedDocument("doc-completed", 0));
        documentRepository.save(document("doc-queued", 1));
        CountingDocumentTextExtractor extractor = new CountingDocumentTextExtractor();
        BatchProcessingUseCase useCase = useCase(documentRepository, extractor);

        useCase.processBatch("batch-test");

        assertThat(extractor.processedDocumentIds).containsExactly("doc-queued");
    }

    /**
     * 提取中失败时应保留已上报的图片页进度，便于 Dashboard 定位失败位置。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void processBatchKeepsLatestProgressWhenExtractionFails() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        documentRepository.save(document("doc-1", 0));
        BatchProcessingUseCase useCase = useCase(documentRepository, new FailingDocumentTextExtractor());

        useCase.processBatch("batch-test");

        assertThat(documentRepository.findById("doc-1")).get().satisfies(document -> {
            assertThat(document.status()).isEqualTo(DocumentStatus.FAILED);
            assertThat(document.stage()).isEqualTo(ProcessingStage.OCR_IMAGES);
            assertThat(document.currentPage()).isEqualTo(2);
            assertThat(document.totalPages()).isEqualTo(5);
        });
    }

    /**
     * 文档完成事件应携带解析完成回调 body 契约。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void processBatchBuildsCompletedCallbackBodyContract() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrEventRepository eventRepository = new InMemoryOcrEventRepository();
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository();
        documentRepository.save(document("doc-1", 0, new JsonPayload(Map.of("source", "upload-form"))));
        batchRepository.save(Batch.create("batch-test", 1, new JsonPayload(Map.of("source", "upload-form")),
                Optional.of("https://callback.example.test/done"), Optional.of("idem-001"), OffsetDateTime.now()));
        BatchProcessingUseCase useCase = useCase(documentRepository, eventRepository, batchRepository,
                new FixedTextExtractor("markdown text"));

        useCase.processBatch("batch-test");

        assertThat(eventRepository.events)
                .filteredOn(event -> DocLensConstants.EVENT_DOCUMENT_COMPLETED.equals(event.eventType()))
                .singleElement()
                .satisfies(event -> assertThat(event.resultSummary()).containsEntry("callback_body",
                        Map.of("meta", Map.of("source", "upload-form"), "text", "markdown text",
                                "idempotency_key", "idem-001")));
    }

    /**
     * 配置 LLM 后处理时应优先保存 Markdown，并保留原始 OCR 文本。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void processBatchUsesLlmMarkdownWhenConfigured() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        InMemoryOcrEventRepository eventRepository = new InMemoryOcrEventRepository();
        documentRepository.save(document("doc-1", 0, new JsonPayload(Map.of("kind", "invoice"))));
        BatchProcessingUseCase useCase = useCase(documentRepository, resultRepository, eventRepository,
                new InMemoryBatchRepository(), new FixedTextExtractor("原始 OCR 文本"),
                new FixedMarkdownPostProcessor("# 发票\n\n原始 OCR 文本"));

        useCase.processBatch("batch-test");

        assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
            assertThat(result.finalText()).isEqualTo("# 发票\n\n原始 OCR 文本");
            assertThat(result.rawVendorOutput())
                    .containsEntry("ocr_text", "原始 OCR 文本")
                    .containsEntry("llm_markdown_applied", true);
        });
        assertThat(eventRepository.events)
                .filteredOn(event -> DocLensConstants.EVENT_DOCUMENT_COMPLETED.equals(event.eventType()))
                .singleElement()
                .satisfies(event -> assertThat(event.resultSummary()).extracting("callback_body")
                        .isEqualTo(Map.of("meta", Map.of("kind", "invoice"), "text", "# 发票\n\n原始 OCR 文本",
                                "idempotency_key", "")));
    }

    /**
     * LLM 返回成对 think 标签时应剥离思考过程，只保存最终 Markdown。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void processBatchRemovesThinkBlockFromLlmMarkdown() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        documentRepository.save(document("doc-1", 0));
        BatchProcessingUseCase useCase = useCase(documentRepository, resultRepository, new InMemoryOcrEventRepository(),
                new InMemoryBatchRepository(), new FixedTextExtractor("原始 OCR 文本"),
                new FixedMarkdownPostProcessor("<think>\nI should analyze the OCR.\n</think>\n# 正文\n\n原始 OCR 文本"));

        useCase.processBatch("batch-test");

        assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
            assertThat(result.finalText()).isEqualTo("# 正文\n\n原始 OCR 文本");
            assertThat(result.finalText()).doesNotContain("<think>").doesNotContain("I should analyze");
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
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        documentRepository.save(document("doc-1", 0));
        BatchProcessingUseCase useCase = useCase(documentRepository, resultRepository, new InMemoryOcrEventRepository(),
                new InMemoryBatchRepository(), new FixedTextExtractor("原始 OCR 文本"),
                new FixedMarkdownPostProcessor("<think>\nThe user wants me to convert OCR text into Markdown."));

        useCase.processBatch("batch-test");

        assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
            assertThat(result.finalText()).isEqualTo("原始 OCR 文本");
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
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        documentRepository.save(document("doc-1", 0));
        BatchProcessingUseCase useCase = useCase(documentRepository, resultRepository, new InMemoryOcrEventRepository(),
                new InMemoryBatchRepository(), new FixedTextExtractor("原始 OCR 文本"),
                new FailingMarkdownPostProcessor());

        useCase.processBatch("batch-test");

        assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
            assertThat(result.finalText()).isEqualTo("原始 OCR 文本");
            assertThat(result.warnings()).contains("llm_markdown_post_processing_failed");
            assertThat(result.rawVendorOutput())
                    .containsEntry("llm_markdown_applied", false)
                    .containsEntry("llm_error_message", "llm unavailable");
        });
    }

    /**
     * LLM 第三次重试成功时不应回退 OCR。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void processBatchKeepsMarkdownWhenLlmSucceedsOnThirdAttempt() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        RetryingMarkdownPostProcessor markdownPostProcessor = new RetryingMarkdownPostProcessor(2, "# 修复后 Markdown");
        documentRepository.save(document("doc-1", 0));
        BatchProcessingUseCase useCase = useCase(documentRepository, resultRepository, new InMemoryOcrEventRepository(),
                new InMemoryBatchRepository(), new FixedTextExtractor("原始 OCR 文本"), markdownPostProcessor);

        useCase.processBatch("batch-test");

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
     * @date 2026-06-10
     */
    @Test
    void processBatchFallsBackAfterThreeLlmFailures() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        CountingFailingMarkdownPostProcessor markdownPostProcessor =
                new CountingFailingMarkdownPostProcessor("llm unavailable");
        documentRepository.save(document("doc-1", 0));
        BatchProcessingUseCase useCase = useCase(documentRepository, resultRepository, new InMemoryOcrEventRepository(),
                new InMemoryBatchRepository(), new FixedTextExtractor("原始 OCR 文本"), markdownPostProcessor);

        useCase.processBatch("batch-test");

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
     * @date 2026-06-09
     */
    @Test
    void processBatchMarksLlmMarkdownAsNotAppliedWhenNoopProcessorIsUsed() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        documentRepository.save(document("doc-1", 0));
        BatchProcessingUseCase useCase = useCase(documentRepository, resultRepository, new InMemoryOcrEventRepository(),
                new InMemoryBatchRepository(), new FixedTextExtractor("原始 OCR 文本"), MarkdownPostProcessor.noop());

        useCase.processBatch("batch-test");

        assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
            assertThat(result.finalText()).isEqualTo("原始 OCR 文本");
            assertThat(result.rawVendorOutput()).containsEntry("llm_markdown_applied", false);
        });
    }

    /**
     * 创建批次处理用例。
     *
     * @param documentRepository 文档仓储
     * @param extractor 文本提取器
     * @return 批次处理用例
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private BatchProcessingUseCase useCase(
            InMemoryDocumentJobRepository documentRepository,
            DocumentTextExtractor extractor
    ) {
        return useCase(documentRepository, extractor, new InlineExecutorService());
    }

    /**
     * 创建批次处理用例。
     *
     * @param documentRepository 文档仓储
     * @param extractor 文本提取器
     * @param documentExecutor 文档处理线程池
     * @return 批次处理用例
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private BatchProcessingUseCase useCase(
            InMemoryDocumentJobRepository documentRepository,
            DocumentTextExtractor extractor,
            ExecutorService documentExecutor
    ) {
        return useCase(documentRepository, new InMemoryOcrEventRepository(), new InMemoryBatchRepository(), extractor,
                documentExecutor);
    }

    /**
     * 创建批次处理用例。
     *
     * @param documentRepository 文档仓储
     * @param eventRepository 事件仓储
     * @param batchRepository 批次仓储
     * @param extractor 文本提取器
     * @return 批次处理用例
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private BatchProcessingUseCase useCase(
            InMemoryDocumentJobRepository documentRepository,
            InMemoryOcrEventRepository eventRepository,
            InMemoryBatchRepository batchRepository,
            DocumentTextExtractor extractor
    ) {
        return useCase(documentRepository, eventRepository, batchRepository, extractor, new InlineExecutorService());
    }

    /**
     * 创建批次处理用例。
     *
     * @param documentRepository 文档仓储
     * @param eventRepository 事件仓储
     * @param batchRepository 批次仓储
     * @param extractor 文本提取器
     * @param documentExecutor 文档处理线程池
     * @return 批次处理用例
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private BatchProcessingUseCase useCase(
            InMemoryDocumentJobRepository documentRepository,
            InMemoryOcrEventRepository eventRepository,
            InMemoryBatchRepository batchRepository,
            DocumentTextExtractor extractor,
            ExecutorService documentExecutor
    ) {
        return useCase(new UseCaseTestConfig(documentRepository, new InMemoryOcrResultRepository(), eventRepository,
                batchRepository, extractor, MarkdownPostProcessor.noop(), documentExecutor));
    }

    /**
     * 创建批次处理用例。
     *
     * @param documentRepository 文档仓储
     * @param resultRepository 结果仓储
     * @param eventRepository 事件仓储
     * @param batchRepository 批次仓储
     * @param extractor 文本提取器
     * @param markdownPostProcessor Markdown 后处理器
     * @return 批次处理用例
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private BatchProcessingUseCase useCase(
            InMemoryDocumentJobRepository documentRepository,
            InMemoryOcrResultRepository resultRepository,
            InMemoryOcrEventRepository eventRepository,
            InMemoryBatchRepository batchRepository,
            DocumentTextExtractor extractor,
            MarkdownPostProcessor markdownPostProcessor
    ) {
        return useCase(new UseCaseTestConfig(documentRepository, resultRepository, eventRepository, batchRepository,
                extractor, markdownPostProcessor, new InlineExecutorService()));
    }

    /**
     * 创建批次处理用例。
     *
     * @param config 测试用例配置
     * @return 批次处理用例
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private BatchProcessingUseCase useCase(UseCaseTestConfig config) {
        BatchProcessingDependencies dependencies = new BatchProcessingDependencies(config.documentRepository(),
                config.resultRepository(), config.eventRepository(), config.batchRepository(), new DefaultAdapterRegistry(
                List.of(new StubAdapter())), new InMemoryObjectStorage(), config.extractor(), new IdGenerator(),
                new OcrEventFactory(new IdGenerator()), config.markdownPostProcessor(), config.documentExecutor());
        return new BatchProcessingUseCase(dependencies, new InlineTransactionRunner());
    }

    /**
     * 关闭测试线程池。
     *
     * @param executor 待关闭线程池
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdownNow();
    }

    /**
     * 创建测试文档。
     *
     * @param documentId 文档 ID
     * @param sortOrder 排序
     * @return 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private DocumentJob document(String documentId, int sortOrder) {
        return document(documentId, sortOrder, JsonPayload.empty());
    }

    /**
     * 创建测试文档。
     *
     * @param documentId 文档 ID
     * @param sortOrder 排序
     * @param metadata 文档元数据
     * @return 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private DocumentJob document(String documentId, int sortOrder, JsonPayload metadata) {
        DocumentJobCreateRequest request = new DocumentJobCreateRequest(documentId, "batch-test",
                documentId + ".txt", DocumentType.TEXT, 5, 1, "local://" + documentId, "stub_ocr",
                Optional.empty(), metadata, sortOrder, OffsetDateTime.now());
        return DocumentJob.create(request);
    }

    /**
     * 创建已完成的测试文档。
     *
     * @param documentId 文档 ID
     * @param sortOrder 排序
     * @return 已完成文档
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentJob completedDocument(String documentId, int sortOrder) {
        OffsetDateTime now = OffsetDateTime.now();
        return document(documentId, sortOrder).startProcessing(now).complete("result-" + documentId, now.plusSeconds(1));
    }

    /**
     * 批次处理用例测试配置。
     *
     * @param documentRepository 文档仓储
     * @param resultRepository 结果仓储
     * @param eventRepository 事件仓储
     * @param batchRepository 批次仓储
     * @param extractor 文本提取器
     * @param markdownPostProcessor Markdown 后处理器
     * @param documentExecutor 文档处理线程池
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private record UseCaseTestConfig(
            InMemoryDocumentJobRepository documentRepository,
            InMemoryOcrResultRepository resultRepository,
            InMemoryOcrEventRepository eventRepository,
            InMemoryBatchRepository batchRepository,
            DocumentTextExtractor extractor,
            MarkdownPostProcessor markdownPostProcessor,
            ExecutorService documentExecutor
    ) {
    }

    /**
     * 记录进度实时可见性的提取器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class RecordingDocumentTextExtractor implements DocumentTextExtractor {

        private final DocumentJobRepository documentRepository;
        private boolean stageWasVisibleDuringExtraction;
        private boolean currentPageWasVisibleDuringExtraction;

        /**
         * 创建记录型文本提取器。
         *
         * @param documentRepository 文档仓储
         * @author lvdaxianerplus
         * @date 2026-06-08
         */
        RecordingDocumentTextExtractor(DocumentJobRepository documentRepository) {
            this.documentRepository = documentRepository;
        }

        @Override
        public DocumentTextExtractionResult extract(DocumentTextExtractionRequest request) {
            if ("doc-1".equals(request.document().documentId())) {
                // 观测文档主动上报进度，用于验证进度落库实时可见。
                request.progressReporter().report(ProcessingStage.OCR_IMAGES, 2, 5);
                stageWasVisibleDuringExtraction = documentRepository.findById("doc-1")
                        .filter(document -> document.stage() == ProcessingStage.OCR_IMAGES)
                        .isPresent();
                currentPageWasVisibleDuringExtraction = documentRepository.findById("doc-1")
                        .filter(document -> document.currentPage() == 2)
                        .filter(document -> document.totalPages() == 5)
                        .isPresent();
            } else {
                // 非观测文档只返回文本，避免把并发语义锁回文档串行。
            }
            return DocumentTextExtractionResult.plainText(request.document().documentId(),
                    request.document().fileName(), "text-" + request.document().documentId());
        }
    }

    /**
     * 先上报进度再失败的文本提取器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class FailingDocumentTextExtractor implements DocumentTextExtractor {

        @Override
        public DocumentTextExtractionResult extract(DocumentTextExtractionRequest request) {
            request.progressReporter().report(ProcessingStage.OCR_IMAGES, 2, 5);
            throw new IllegalStateException("ocr failed");
        }
    }

    /**
     * 阻塞型文本提取器，用于验证批次是否能同时启动多个文档。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static class BlockingDocumentTextExtractor implements DocumentTextExtractor {

        private final CountDownLatch entered = new CountDownLatch(CONCURRENT_TEST_DOCUMENTS);
        private final CountDownLatch release = new CountDownLatch(1);

        /**
         * 等待两个文档都进入提取器。
         *
         * @return 两个文档是否都进入提取器
         * @throws InterruptedException 等待被中断时抛出
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        boolean awaitBothEntered() throws InterruptedException {
            return entered.await(CONCURRENT_TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        /**
         * 释放阻塞中的文档提取。
         *
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        void release() {
            release.countDown();
        }

        @Override
        public DocumentTextExtractionResult extract(DocumentTextExtractionRequest request) {
            entered.countDown();
            awaitRelease();
            return DocumentTextExtractionResult.plainText(request.document().documentId(),
                    request.document().fileName(), "text-" + request.document().documentId());
        }

        /**
         * 等待测试释放提取流程。
         *
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        private void awaitRelease() {
            try {
                release.await(CONCURRENT_TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("blocking extractor interrupted", ex);
            }
        }
    }

    /**
     * 首个文档阻塞、第二个文档快速完成的提取器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static class SlowFirstDocumentTextExtractor implements DocumentTextExtractor {

        private final CountDownLatch releaseSlow = new CountDownLatch(1);
        private final CountDownLatch fastCompleted = new CountDownLatch(1);

        /**
         * 等待快文档完成提取。
         *
         * @return 快文档是否完成提取
         * @throws InterruptedException 等待被中断时抛出
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        boolean awaitFastCompleted() throws InterruptedException {
            return fastCompleted.await(CONCURRENT_TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        /**
         * 释放慢文档提取。
         *
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        void releaseSlow() {
            releaseSlow.countDown();
        }

        @Override
        public DocumentTextExtractionResult extract(DocumentTextExtractionRequest request) {
            if ("doc-slow".equals(request.document().documentId())) {
                // 慢文档阻塞时，快文档仍应能完成并先落库。
                awaitSlowRelease();
            } else {
                // 快文档完成信号用于测试观察持久化是否被慢文档阻塞。
                fastCompleted.countDown();
            }
            return DocumentTextExtractionResult.plainText(request.document().documentId(),
                    request.document().fileName(), "text-" + request.document().documentId());
        }

        /**
         * 等待慢文档释放。
         *
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        private void awaitSlowRelease() {
            try {
                releaseSlow.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("slow extractor interrupted", ex);
            }
        }
    }

    /**
     * 返回固定文本的提取器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static class FixedTextExtractor implements DocumentTextExtractor {

        private final String text;

        /**
         * 创建固定文本提取器。
         *
         * @param text 固定文本
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        FixedTextExtractor(String text) {
            this.text = text;
        }

        @Override
        public DocumentTextExtractionResult extract(DocumentTextExtractionRequest request) {
            return DocumentTextExtractionResult.plainText(request.document().documentId(),
                    request.document().fileName(), text);
        }
    }

    /**
     * 记录实际被处理文档 ID 的提取器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static class CountingDocumentTextExtractor implements DocumentTextExtractor {

        private final List<String> processedDocumentIds = new ArrayList<>();

        @Override
        public DocumentTextExtractionResult extract(DocumentTextExtractionRequest request) {
            processedDocumentIds.add(request.document().documentId());
            return DocumentTextExtractionResult.plainText(request.document().documentId(),
                    request.document().fileName(), "text-" + request.document().documentId());
        }
    }

    /**
     * 返回固定 Markdown 的 LLM 后处理器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private record FixedMarkdownPostProcessor(String markdown) implements MarkdownPostProcessor {

        @Override
        public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
            return MarkdownPostProcessingResult.markdown(markdown);
        }
    }

    /**
     * 固定失败的 LLM 后处理器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static class FailingMarkdownPostProcessor implements MarkdownPostProcessor {

        @Override
        public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
            throw new IllegalStateException("llm unavailable");
        }
    }

    /**
     * 前若干次失败、随后成功的 LLM 后处理器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static class RetryingMarkdownPostProcessor implements MarkdownPostProcessor {

        private final int failTimes;
        private final String markdown;
        private int attempts;

        /**
         * 创建重试型 LLM 后处理器。
         *
         * @param failTimes 前置失败次数
         * @param markdown 成功后返回的 Markdown
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        RetryingMarkdownPostProcessor(int failTimes, String markdown) {
            this.failTimes = failTimes;
            this.markdown = markdown;
        }

        /**
         * 执行一次 Markdown 后处理尝试。
         *
         * @param request Markdown 后处理请求
         * @return 成功时返回 Markdown 结果，失败时抛出异常
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        @Override
        public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
            attempts++;
            if (attempts <= failTimes) {
                throw new IllegalStateException("transient llm failure");
            }
            return MarkdownPostProcessingResult.markdown(markdown);
        }

        /**
         * 获取已执行的尝试次数。
         *
         * @return 尝试次数
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        int attempts() {
            return attempts;
        }
    }

    /**
     * 始终失败并记录次数的 LLM 后处理器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static class CountingFailingMarkdownPostProcessor implements MarkdownPostProcessor {

        private final String message;
        private int attempts;

        /**
         * 创建始终失败的 LLM 后处理器。
         *
         * @param message 失败消息
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        CountingFailingMarkdownPostProcessor(String message) {
            this.message = message;
        }

        /**
         * 执行一次始终失败的 Markdown 后处理尝试。
         *
         * @param request Markdown 后处理请求
         * @return 不会返回成功结果
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        @Override
        public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
            attempts++;
            throw new IllegalStateException(message);
        }

        /**
         * 获取已执行的尝试次数。
         *
         * @return 尝试次数
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        int attempts() {
            return attempts;
        }
    }

    /**
     * 内存文档仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class InMemoryDocumentJobRepository implements DocumentJobRepository {

        private final Map<String, DocumentJob> documents = new ConcurrentHashMap<>(TEST_DOCUMENT_CAPACITY);

        @Override
        public void save(DocumentJob document) {
            documents.put(document.documentId(), document);
        }

        @Override
        public void saveAll(List<DocumentJob> documents) {
            documents.forEach(this::save);
        }

        @Override
        public void update(DocumentJob document) {
            documents.put(document.documentId(), document);
        }

        @Override
        public void updateAll(List<DocumentJob> documents) {
            documents.forEach(this::update);
        }

        @Override
        public Optional<DocumentJob> findById(String documentId) {
            return Optional.ofNullable(documents.get(documentId));
        }

        @Override
        public List<DocumentJob> listByBatchId(String batchId) {
            return documents.values().stream()
                    .filter(document -> batchId.equals(document.batchId()))
                    .sorted((left, right) -> Integer.compare(left.sortOrder(), right.sortOrder()))
                    .toList();
        }

        @Override
        public List<DocumentJob> listByBatchIds(List<String> batchIds) {
            return documents.values().stream().filter(document -> batchIds.contains(document.batchId())).toList();
        }

        @Override
        public List<DocumentJob> listRecent(int limit) {
            return documents.values().stream().limit(limit).toList();
        }
    }

    /**
     * 内存 OCR 结果仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class InMemoryOcrResultRepository implements OcrResultRepository {

        private final Map<String, OcrResult> results = new ConcurrentHashMap<>(TEST_DOCUMENT_CAPACITY);
        private final Map<String, CountDownLatch> resultSignals = new ConcurrentHashMap<>(TEST_DOCUMENT_CAPACITY);

        @Override
        public void save(OcrResult result) {
            results.put(result.documentId(), result);
            signalResult(result.documentId());
        }

        @Override
        public void saveAll(List<OcrResult> results) {
            results.forEach(this::save);
        }

        @Override
        public Optional<OcrResult> findByDocumentId(String documentId) {
            return Optional.ofNullable(results.get(documentId));
        }

        /**
         * 等待指定文档结果落库。
         *
         * @param documentId 文档 ID
         * @return 结果是否已落库
         * @throws InterruptedException 等待被中断时抛出
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        boolean awaitResult(String documentId) throws InterruptedException {
            if (results.containsKey(documentId)) {
                // 结果已存在时直接返回，避免错过先于等待注册发生的保存信号。
                return true;
            } else {
                // 结果尚未保存时注册等待信号，观察完成顺序持久化行为。
                return resultSignals.computeIfAbsent(documentId, ignored -> new CountDownLatch(1))
                        .await(CONCURRENT_TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        }

        /**
         * 通知指定文档结果已落库。
         *
         * @param documentId 文档 ID
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        private void signalResult(String documentId) {
            resultSignals.computeIfAbsent(documentId, ignored -> new CountDownLatch(1)).countDown();
        }
    }

    /**
     * 内存 OCR 事件仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class InMemoryOcrEventRepository implements OcrEventRepository {

        private final List<OcrEvent> events = new ArrayList<>(TEST_EVENT_CAPACITY);

        @Override
        public void save(OcrEvent event) {
            events.add(event);
        }

        @Override
        public void saveAll(List<OcrEvent> events) {
            this.events.addAll(events);
        }

        @Override
        public List<OcrEvent> listByBatchId(String batchId) {
            return events.stream().filter(event -> batchId.equals(event.batchId())).toList();
        }

        @Override
        public List<OcrEvent> listRecent(int limit) {
            return events.stream().limit(limit).toList();
        }
    }

    /**
     * 内存批次仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class InMemoryBatchRepository implements BatchRepository {

        private final Map<String, Batch> batches = new HashMap<>(TEST_DOCUMENT_CAPACITY);

        @Override
        public void save(Batch batch) {
            batches.put(batch.batchId(), batch);
        }

        @Override
        public Optional<Batch> findById(String batchId) {
            return Optional.ofNullable(batches.get(batchId));
        }

        @Override
        public Optional<Batch> findByIdempotencyKey(String idempotencyKey) {
            return Optional.empty();
        }

        @Override
        public List<Batch> listRecent(int limit) {
            return List.of();
        }

        @Override
        public void updateSummary(String batchId, int completedFiles, int failedFiles, BatchStatus status) {
            // 当前测试只验证文档级持久化。
        }
    }

    /**
     * 内存对象存储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class InMemoryObjectStorage implements ObjectStorage {

        @Override
        public String writeBytes(String objectKey, byte[] content) {
            return objectKey;
        }

        @Override
        public byte[] readBytes(String storageUri) {
            return "text".getBytes();
        }
    }

    /**
     * Stub OCR 适配器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class StubAdapter implements OcrAdapter {

        @Override
        public AdapterCapability capability() {
            return new AdapterCapability("stub_ocr", List.of("text"), false, false, true, false,
                    false, null, null, 1, 1, "test");
        }

        @Override
        public ImageOcrResult recognize(ImageOcrRequest request) {
            return ImageOcrResult.fromBlocks(request.pageNo(), Map.of(), List.of(), List.of());
        }
    }

    /**
     * 直接执行事务的测试事务器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class InlineTransactionRunner implements TransactionRunner {

        @Override
        public <T> T requiredResult(java.util.function.Supplier<T> action) {
            return action.get();
        }

        @Override
        public void requiredVoid(Runnable action) {
            action.run();
        }
    }

    /**
     * 直接执行任务的测试线程池，避免普通用例泄漏后台线程。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static class InlineExecutorService extends AbstractExecutorService {

        private boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return shutdown;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}
