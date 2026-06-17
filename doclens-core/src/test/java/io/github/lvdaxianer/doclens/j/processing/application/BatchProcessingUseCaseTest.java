package io.github.lvdaxianer.doclens.j.processing.application;

import static io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCaseTestSupport.CONCURRENT_TEST_DOCUMENTS;
import static io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCaseTestSupport.completedDocument;
import static io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCaseTestSupport.document;
import static io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCaseTestSupport.shutdownExecutor;
import static io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCaseTestSupport.useCase;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

/**
 * 批次处理编排用例测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class BatchProcessingUseCaseTest {

    private static final String CALLER_FIELD = "caller";

    /*
     * 本类只覆盖批次处理编排语义：
     * 文档并发启动、单文档完成可见、失败进度保留和完成事件契约。
     * LLM Markdown 后处理场景已拆到 BatchProcessingUseCaseLlmMarkdownTest，
     * 避免同一个测试类同时承担编排和内容后处理两类职责。
     *
     * 这里保留真实线程池相关用例，因为并发启动和快文档先落库
     * 只有在真实异步执行时才有回归检测价值。
     * 普通场景仍使用 InlineExecutor，避免单元测试泄漏后台线程。
     *
     * 断言优先检查仓储最终状态和事件契约，
     * 不直接断言 BatchProcessingUseCase 内部私有方法，
     * 这样测试能跟随实现拆分保持稳定。
     *
     * 每个测试只保留一个业务断言主题，
     * 公共构造、内存仓储和提取器替身全部放在同包支撑类，
     * 让本文件保持在“读测试行为”而不是“读测试基础设施”。
     * 如果新增编排测试，优先复用支撑类而不是在本类新增内部类。
     * 如果新增 LLM 内容后处理测试，应放到 Markdown 专用测试类。
     */

    /**
     * 每个文档完成后应持久化，避免长批次结束后才统一可见。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void processBatchPersistsEachCompletedDocument() {
        // 准备两个排队文档，模拟一个批次内存在多个待处理任务。
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        documentRepository.saveAll(List.of(document("doc-1", 0), document("doc-2", 1)));
        // 使用会读取仓储实时状态的提取器，避免只验证最终内存对象。
        RecordingDocumentTextExtractor extractor = new RecordingDocumentTextExtractor(documentRepository);
        BatchProcessingUseCase batchUseCase = useCase(documentRepository, extractor);

        // 执行批次处理，触发每个文档独立完成和落库。
        batchUseCase.processBatch("batch-test");

        // 断言每个文档都已完成，避免回归为批次末尾一次性持久化。
        assertThat(documentRepository.findById("doc-1")).get().extracting(DocumentJob::status)
                .isEqualTo(DocumentStatus.COMPLETED);
        // 第二个文档也必须独立完成，覆盖批量保存中遗漏后续文档的风险。
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
        // 准备两个文档和阻塞提取器，用于观测第二个文档是否能进入执行。
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        documentRepository.saveAll(List.of(document("doc-1", 0), document("doc-2", 1)));
        BlockingDocumentTextExtractor extractor = new BlockingDocumentTextExtractor();
        // 使用真实固定线程池验证并发行为，避免 InlineExecutor 掩盖串行问题。
        ExecutorService documentExecutor = Executors.newFixedThreadPool(CONCURRENT_TEST_DOCUMENTS);
        BatchProcessingUseCase batchUseCase = useCase(documentRepository, extractor, documentExecutor);
        ExecutorService batchCaller = Executors.newSingleThreadExecutor();

        try {
            // 在独立线程触发批次处理，让测试线程可以观察两个文档进入情况。
            batchCaller.submit(() -> batchUseCase.processBatch("batch-test"));

            // 两个文档都进入提取器后才释放，证明没有等待首个文档完成。
            assertThat(extractor.awaitBothEntered()).isTrue();
            // 释放阻塞后批次线程可以正常收尾。
            extractor.release();
        } finally {
            // 无论断言是否失败都释放并关闭线程池，避免测试进程残留后台线程。
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
        // 准备慢文档和快文档，模拟同批次内完成顺序与排序顺序不同。
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        documentRepository.saveAll(List.of(document("doc-slow", 0), document("doc-fast", 1)));
        // 结果仓储带有 latch，可以观测快文档是否已经真实保存。
        InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
        SlowFirstDocumentTextExtractor extractor = new SlowFirstDocumentTextExtractor();
        // 使用两个工作线程，让快文档可以绕过慢文档先完成。
        ExecutorService documentExecutor = Executors.newFixedThreadPool(CONCURRENT_TEST_DOCUMENTS);
        BatchProcessingUseCase batchUseCase = useCase(BatchProcessingUseCaseConfig.of(documentRepository,
                resultRepository, extractor, documentExecutor));
        ExecutorService batchCaller = Executors.newSingleThreadExecutor();

        try {
            // 异步启动批次处理，测试线程等待快文档的完成信号。
            batchCaller.submit(() -> batchUseCase.processBatch("batch-test"));

            // 快文档结果必须先落库，慢文档仍处于阻塞态时不能已有结果。
            assertThat(extractor.awaitFastCompleted()).isTrue();
            assertThat(resultRepository.awaitResult("doc-fast")).isTrue();
            // 慢文档未释放前不能提前出现结果，证明快慢文档持久化互不阻塞。
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
        // 准备单文档批次，提取器会在处理过程中主动上报阶段进度。
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        documentRepository.save(document("doc-1", 0));
        RecordingDocumentTextExtractor extractor = new RecordingDocumentTextExtractor(documentRepository);
        BatchProcessingUseCase batchUseCase = useCase(documentRepository, extractor);

        // 执行过程中，提取器内部会立即读取仓储确认进度是否可见。
        batchUseCase.processBatch("batch-test");

        // 进度必须在提取期间可见，最终阶段也应落到 COMPLETED。
        assertThat(extractor.stageWasVisibleDuringExtraction()).isTrue();
        // 当前页和总页数是 Dashboard 判断卡点的最小数据。
        assertThat(extractor.currentPageWasVisibleDuringExtraction()).isTrue();
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
        // 准备一个已完成文档和一个排队文档，验证批次处理不会重复跑完成项。
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        // 已完成文档代表重试时同批次历史结果。
        documentRepository.save(completedDocument("doc-completed", 0));
        // 排队文档代表本次真正应处理的任务。
        documentRepository.save(document("doc-queued", 1));
        CountingDocumentTextExtractor extractor = new CountingDocumentTextExtractor();
        BatchProcessingUseCase batchUseCase = useCase(documentRepository, extractor);

        // 执行批次处理后，提取器只应收到仍处于 queued 的文档。
        batchUseCase.processBatch("batch-test");

        // 已完成文档被跳过，避免文档级重试影响同批次其它历史结果。
        assertThat(extractor.processedDocumentIds()).containsExactly("doc-queued");
    }

    /**
     * 提取中失败时应保留已上报的图片页进度，便于 Dashboard 定位失败位置。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void processBatchKeepsLatestProgressWhenExtractionFails() {
        // 准备会在上报页进度之后抛错的提取器。
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        // 单文档即可覆盖失败时进度保留语义。
        documentRepository.save(document("doc-1", 0));
        BatchProcessingUseCase batchUseCase = useCase(documentRepository, new FailingDocumentTextExtractor());

        // 执行失败路径，验证失败时不会抹掉最后一次进度。
        batchUseCase.processBatch("batch-test");

        // 失败状态、阶段和页码都应保留，用于 Dashboard 定位卡点。
        assertThat(documentRepository.findById("doc-1")).get().satisfies(document -> {
            assertThat(document.status()).isEqualTo(DocumentStatus.FAILED);
            // 阶段停在 OCR_IMAGES，说明失败位置没有被最终状态覆盖。
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
        // 准备带 meta、callbackUrl 和幂等键的批次，覆盖回调 body 契约。
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrEventRepository eventRepository = new InMemoryOcrEventRepository();
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository();
        // 文档 meta 会透传到 callback body。
        documentRepository.save(document("doc-1", 0, new JsonPayload(Map.of("source", "upload-form"))));
        // 批次 callbackUrl 和幂等键会参与完成事件构建。
        batchRepository.save(Batch.create("batch-test", 1, new JsonPayload(Map.of("source", "upload-form")),
                Optional.of("https://callback.example.test/done"), Optional.of("idem-001"), OffsetDateTime.now()));
        BatchProcessingUseCase batchUseCase = useCase(BatchProcessingUseCaseConfig.of(
                new BatchProcessingUseCaseEventConfig(documentRepository, eventRepository, batchRepository,
                        new FixedTextExtractor("markdown text"))));

        // 执行批次后，完成事件应携带外部回调需要的完整 body。
        batchUseCase.processBatch("batch-test");

        // 回调 body 必须包含 meta、最终文本和幂等键，保证调用方可直接消费。
        assertThat(eventRepository.events())
                .filteredOn(event -> DocLensConstants.EVENT_DOCUMENT_COMPLETED.equals(event.eventType()))
                .singleElement()
                .satisfies(event -> assertThat(event.resultSummary()).containsEntry("callback_body",
                        Map.of("meta", Map.of("source", "upload-form"), "text", "markdown text",
                                "idempotency_key", "idem-001", CALLER_FIELD, anonymousCaller())));
    }

    /**
     * 批次配置回调地址时，文档完成后应创建可投递回调任务。
     *
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @Test
    void processBatchCreatesCallbackJobFromCompletedEventWhenCallbackUrlExists() {
        // 准备带 callbackUrl 的批次和回调任务仓储，覆盖完成事件到投递任务的桥接。
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        InMemoryOcrEventRepository eventRepository = new InMemoryOcrEventRepository();
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository();
        InMemoryCallbackJobRepository callbackJobRepository = new InMemoryCallbackJobRepository();
        documentRepository.save(document("doc-1", 0, new JsonPayload(Map.of("source", "upload-form"))));
        batchRepository.save(Batch.create("batch-test", 1, new JsonPayload(Map.of("source", "upload-form")),
                Optional.of("https://callback.example.test/done"), Optional.of("idem-001"), OffsetDateTime.now()));
        BatchProcessingUseCase batchUseCase = useCase(BatchProcessingUseCaseConfig.of(
                new BatchProcessingUseCaseEventConfig(documentRepository, eventRepository, batchRepository,
                        new FixedTextExtractor("markdown text"), callbackJobRepository)));

        // 执行批次后，完成事件应同步生成一条待投递回调任务。
        batchUseCase.processBatch("batch-test");

        // callback job 是 worker 执行回调的唯一入口，必须携带完成事件和 callback_body。
        Map<String, Object> expectedPayload = Map.of("meta", Map.of("source", "upload-form"), "text",
                "markdown text", "idempotency_key", "idem-001", CALLER_FIELD, anonymousCaller());
        assertThat(callbackJobRepository.listByBatchId("batch-test"))
                .singleElement()
                .satisfies(job -> {
                    assertThat(job.callbackUrl()).isEqualTo("https://callback.example.test/done");
                    assertThat(job.documentId()).contains("doc-1");
                    assertThat(job.status()).isEqualTo(CallbackJobStatus.PENDING);
                    assertThat(job.retryCount()).isZero();
                    assertThat(job.payload()).isEqualTo(expectedPayload);
                    assertThat(eventRepository.events()).filteredOn(event -> event.eventId().equals(job.eventId()))
                            .singleElement()
                            .extracting(event -> event.resultSummary().get("callback_body"))
                            .isEqualTo(expectedPayload);
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
}
