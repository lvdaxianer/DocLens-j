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
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTask;
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

    /**
     * 每个文档完成后应立即持久化，避免长批次中途状态不可见。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void processBatchPersistsEachDocumentBeforeProcessingNextOne() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        documentRepository.saveAll(List.of(document("doc-1", 0), document("doc-2", 1)));
        RecordingDocumentTextExtractor extractor = new RecordingDocumentTextExtractor(documentRepository);
        BatchProcessingUseCase useCase = useCase(documentRepository, extractor);

        useCase.processBatch("batch-test");

        assertThat(extractor.secondDocumentSawFirstCompleted).isTrue();
        assertThat(documentRepository.findById("doc-1")).get().extracting(DocumentJob::status)
                .isEqualTo(DocumentStatus.COMPLETED);
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
     * 图片类文档应只入页任务队列，不在批次入口同步执行 OCR。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void processBatchQueuesImageDocumentPagesWithoutSynchronousOcr() {
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryDocumentPageTaskRepository pageTaskRepository = new InMemoryDocumentPageTaskRepository();
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository();
        FailingDocumentTextExtractor extractor = new FailingDocumentTextExtractor();
        batchRepository.save(batch("batch-test", 1));
        documentRepository.save(document("doc-image", 0, DocumentType.IMAGE));
        BatchProcessingUseCase useCase = useCase(documentRepository, batchRepository, pageTaskRepository, extractor);

        useCase.processBatch("batch-test");

        assertThat(pageTaskRepository.listByDocumentId("doc-image"))
                .extracting(DocumentPageTask::pageNo)
                .containsExactly(1);
        assertThat(documentRepository.findById("doc-image")).get().satisfies(document -> {
            assertThat(document.status()).isEqualTo(DocumentStatus.PROCESSING);
            assertThat(document.stage()).isEqualTo(ProcessingStage.OCR_QUEUED);
        });
        assertThat(batchRepository.findById("batch-test")).get().extracting(Batch::status)
                .isEqualTo(BatchStatus.PROCESSING);
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
        return useCase(documentRepository, new InMemoryOcrEventRepository(), new InMemoryBatchRepository(), extractor);
    }

    /**
     * 创建带页任务仓储的批次处理用例。
     *
     * @param documentRepository 文档仓储
     * @param pageTaskRepository 页任务仓储
     * @param extractor 文本提取器
     * @return 批次处理用例
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private BatchProcessingUseCase useCase(
            InMemoryDocumentJobRepository documentRepository,
            InMemoryDocumentPageTaskRepository pageTaskRepository,
            DocumentTextExtractor extractor
    ) {
        return useCase(documentRepository, new InMemoryBatchRepository(), pageTaskRepository, extractor);
    }

    /**
     * 创建带页任务仓储和批次仓储的批次处理用例。
     *
     * @param documentRepository 文档仓储
     * @param batchRepository 批次仓储
     * @param pageTaskRepository 页任务仓储
     * @param extractor 文本提取器
     * @return 批次处理用例
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private BatchProcessingUseCase useCase(
            InMemoryDocumentJobRepository documentRepository,
            InMemoryBatchRepository batchRepository,
            InMemoryDocumentPageTaskRepository pageTaskRepository,
            DocumentTextExtractor extractor
    ) {
        BatchProcessingDependencies dependencies = dependencies(new TestBatchProcessingDependencies(documentRepository,
                new InMemoryOcrResultRepository(), new InMemoryOcrEventRepository(), batchRepository, extractor,
                MarkdownPostProcessor.noop(), pageTaskRepository));
        return new BatchProcessingUseCase(dependencies, new InlineTransactionRunner());
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
        return useCase(documentRepository, new InMemoryOcrResultRepository(), eventRepository, batchRepository,
                extractor, MarkdownPostProcessor.noop());
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
        BatchProcessingDependencies dependencies = dependencies(new TestBatchProcessingDependencies(documentRepository,
                resultRepository, eventRepository, batchRepository, extractor, markdownPostProcessor,
                new InMemoryDocumentPageTaskRepository()));
        return new BatchProcessingUseCase(dependencies, new InlineTransactionRunner());
    }

    /**
     * 创建批次处理依赖。
     *
     * @param dependencies 测试依赖
     * @return 批次处理依赖
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private BatchProcessingDependencies dependencies(TestBatchProcessingDependencies dependencies) {
        PageImagePreparation pageImagePreparation = document -> List.of(new PageImageRef(1, document.storageUri()));
        DocumentPageTaskPreparationService preparationService = new DocumentPageTaskPreparationService(
                dependencies.documentRepository(), dependencies.pageTaskRepository(), pageImagePreparation,
                new IdGenerator());
        return new BatchProcessingDependencies(dependencies.documentRepository(), dependencies.resultRepository(),
                dependencies.eventRepository(), dependencies.batchRepository(),
                new DefaultAdapterRegistry(List.of(new StubAdapter())), new InMemoryObjectStorage(),
                dependencies.extractor(), new IdGenerator(), new OcrEventFactory(new IdGenerator()),
                dependencies.markdownPostProcessor(),
                preparationService);
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
        return document(documentId, sortOrder, DocumentType.TEXT, metadata);
    }

    /**
     * 创建指定类型的测试文档。
     *
     * @param documentId 文档 ID
     * @param sortOrder 排序
     * @param fileType 文档类型
     * @return 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentJob document(String documentId, int sortOrder, DocumentType fileType) {
        return document(documentId, sortOrder, fileType, JsonPayload.empty());
    }

    /**
     * 创建指定类型和元数据的测试文档。
     *
     * @param documentId 文档 ID
     * @param sortOrder 排序
     * @param fileType 文档类型
     * @param metadata 文档元数据
     * @return 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentJob document(String documentId, int sortOrder, DocumentType fileType, JsonPayload metadata) {
        DocumentJobCreateRequest request = new DocumentJobCreateRequest(documentId, "batch-test",
                documentId + ".txt", fileType, 5, 1, "local://" + documentId, "stub_ocr",
                Optional.empty(), metadata, sortOrder, OffsetDateTime.now());
        return DocumentJob.create(request);
    }

    /**
     * 创建测试批次。
     *
     * @param batchId 批次 ID
     * @param totalFiles 总文件数
     * @return 测试批次
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private Batch batch(String batchId, int totalFiles) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Batch(batchId, BatchStatus.QUEUED, totalFiles, 0, 0, Optional.empty(), Optional.empty(),
                "queued", JsonPayload.empty(), Optional.empty(), Optional.empty(), now, now);
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
     * 测试用批次处理依赖。
     *
     * @param documentRepository 文档仓储
     * @param resultRepository 结果仓储
     * @param eventRepository 事件仓储
     * @param batchRepository 批次仓储
     * @param extractor 文本提取器
     * @param markdownPostProcessor Markdown 后处理器
     * @param pageTaskRepository 页任务仓储
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private record TestBatchProcessingDependencies(
            InMemoryDocumentJobRepository documentRepository,
            InMemoryOcrResultRepository resultRepository,
            InMemoryOcrEventRepository eventRepository,
            InMemoryBatchRepository batchRepository,
            DocumentTextExtractor extractor,
            MarkdownPostProcessor markdownPostProcessor,
            InMemoryDocumentPageTaskRepository pageTaskRepository
    ) {
    }

    /**
     * 记录第二个文档处理时第一个文档状态的提取器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class RecordingDocumentTextExtractor implements DocumentTextExtractor {

        private final DocumentJobRepository documentRepository;
        private boolean secondDocumentSawFirstCompleted;
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
            if ("doc-2".equals(request.document().documentId())) {
                secondDocumentSawFirstCompleted = documentRepository.findById("doc-1")
                        .filter(document -> document.status() == DocumentStatus.COMPLETED)
                        .isPresent();
            } else {
                request.progressReporter().report(ProcessingStage.OCR_IMAGES, 2, 5);
                stageWasVisibleDuringExtraction = documentRepository.findById("doc-1")
                        .filter(document -> document.stage() == ProcessingStage.OCR_IMAGES)
                        .isPresent();
                currentPageWasVisibleDuringExtraction = documentRepository.findById("doc-1")
                        .filter(document -> document.currentPage() == 2)
                        .filter(document -> document.totalPages() == 5)
                        .isPresent();
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

        private final Map<String, DocumentJob> documents = new HashMap<>(TEST_DOCUMENT_CAPACITY);

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

        private final Map<String, OcrResult> results = new HashMap<>(TEST_DOCUMENT_CAPACITY);

        @Override
        public void save(OcrResult result) {
            results.put(result.documentId(), result);
        }

        @Override
        public void saveAll(List<OcrResult> results) {
            results.forEach(this::save);
        }

        @Override
        public Optional<OcrResult> findByDocumentId(String documentId) {
            return Optional.ofNullable(results.get(documentId));
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
            findById(batchId).ifPresent(batch -> batches.put(batchId, new Batch(batch.batchId(), status,
                    batch.totalFiles(), completedFiles, failedFiles, batch.currentDocumentId(),
                    batch.currentDocumentName(), status.name().toLowerCase(), batch.metadata(), batch.callbackUrl(),
                    batch.idempotencyKey(), batch.createdAt(), OffsetDateTime.now())));
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
}
