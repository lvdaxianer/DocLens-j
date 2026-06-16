package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.DefaultAdapterRegistry;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractor;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.domain.EmptyCallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * 批次处理用例测试公共支撑。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class BatchProcessingUseCaseTestSupport {

    static final int TEST_DOCUMENT_CAPACITY = 4;
    static final int TEST_EVENT_CAPACITY = 8;
    static final int CONCURRENT_TEST_DOCUMENTS = 2;
    static final int CONCURRENT_TEST_TIMEOUT_SECONDS = 2;
    static final int PROCESSED_DOCUMENT_ID_CAPACITY = 4;

    /**
     * 禁止实例化测试工具类。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private BatchProcessingUseCaseTestSupport() {
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
    static BatchProcessingUseCase useCase(
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
    static BatchProcessingUseCase useCase(
            InMemoryDocumentJobRepository documentRepository,
            DocumentTextExtractor extractor,
            ExecutorService documentExecutor
    ) {
        return useCase(BatchProcessingUseCaseConfig.of(documentRepository, extractor, documentExecutor));
    }

    /**
     * 创建批次处理用例。
     *
     * @param config 测试用例配置
     * @return 批次处理用例
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static BatchProcessingUseCase useCase(BatchProcessingUseCaseConfig config) {
        BatchProcessingDependencies dependencies = new BatchProcessingDependencies(config.documentRepository(),
                config.resultRepository(), config.eventRepository(), config.batchRepository(), new DefaultAdapterRegistry(
                List.of(new StubAdapter())), new InMemoryObjectStorage(), config.extractor(), new IdGenerator(),
                new OcrEventFactory(new IdGenerator()), config.markdownPostProcessor(), config.documentExecutor(),
                pageTaskPreparationService(config.documentRepository()), config.callbackJobRepository());
        return new BatchProcessingUseCase(dependencies, new InlineTransactionRunner());
    }

    /**
     * 创建测试页任务准备服务。
     *
     * @param documentRepository 文档仓储
     * @return 页任务准备服务
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private static DocumentPageTaskPreparationService pageTaskPreparationService(
            InMemoryDocumentJobRepository documentRepository
    ) {
        PageImagePreparation preparation = ignored -> List.of();
        return new DocumentPageTaskPreparationService(documentRepository, new InMemoryDocumentPageTaskRepository(),
                preparation, new IdGenerator());
    }

    /**
     * 关闭测试线程池。
     *
     * @param executor 待关闭线程池
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static void shutdownExecutor(ExecutorService executor) {
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
    static DocumentJob document(String documentId, int sortOrder) {
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
    static DocumentJob document(String documentId, int sortOrder, JsonPayload metadata) {
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
    static DocumentJob completedDocument(String documentId, int sortOrder) {
        OffsetDateTime now = OffsetDateTime.now();
        return document(documentId, sortOrder).startProcessing(now).complete("result-" + documentId, now.plusSeconds(1));
    }

}

/**
 * 批次处理测试用例配置。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
record BatchProcessingUseCaseConfig(
        InMemoryDocumentJobRepository documentRepository,
        InMemoryOcrResultRepository resultRepository,
        InMemoryOcrEventRepository eventRepository,
        InMemoryBatchRepository batchRepository,
        DocumentTextExtractor extractor,
        MarkdownPostProcessor markdownPostProcessor,
        ExecutorService documentExecutor,
        CallbackJobRepository callbackJobRepository
) {

    /**
     * 使用默认依赖创建测试配置。
     *
     * @param documentRepository 文档仓储
     * @param extractor 文本提取器
     * @param documentExecutor 文档处理线程池
     * @return 测试配置
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static BatchProcessingUseCaseConfig of(
            InMemoryDocumentJobRepository documentRepository,
            DocumentTextExtractor extractor,
            ExecutorService documentExecutor
    ) {
        return new BatchProcessingUseCaseConfig(documentRepository, new InMemoryOcrResultRepository(),
                new InMemoryOcrEventRepository(), new InMemoryBatchRepository(), extractor, MarkdownPostProcessor.noop(),
                documentExecutor, new EmptyCallbackJobRepository());
    }

    /**
     * 使用默认批次依赖创建测试配置。
     *
     * @param documentRepository 文档仓储
     * @param resultRepository 结果仓储
     * @param extractor 文本提取器
     * @param documentExecutor 文档处理线程池
     * @return 测试配置
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static BatchProcessingUseCaseConfig of(
            InMemoryDocumentJobRepository documentRepository,
            InMemoryOcrResultRepository resultRepository,
            DocumentTextExtractor extractor,
            ExecutorService documentExecutor
    ) {
        return new BatchProcessingUseCaseConfig(documentRepository, resultRepository, new InMemoryOcrEventRepository(),
                new InMemoryBatchRepository(), extractor, MarkdownPostProcessor.noop(), documentExecutor,
                new EmptyCallbackJobRepository());
    }

    /**
     * 使用默认事件和批次依赖创建 Markdown 测试配置。
     *
     * @param documentRepository 文档仓储
     * @param resultRepository 结果仓储
     * @param extractor 文本提取器
     * @param markdownPostProcessor Markdown 后处理器
     * @return 测试配置
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static BatchProcessingUseCaseConfig of(
            InMemoryDocumentJobRepository documentRepository,
            InMemoryOcrResultRepository resultRepository,
            DocumentTextExtractor extractor,
            MarkdownPostProcessor markdownPostProcessor
    ) {
        return new BatchProcessingUseCaseConfig(documentRepository, resultRepository, new InMemoryOcrEventRepository(),
                new InMemoryBatchRepository(), extractor, markdownPostProcessor, new InlineExecutorService(),
                new EmptyCallbackJobRepository());
    }

    /**
     * 使用指定事件依赖创建 Markdown 测试配置。
     *
     * @param config 指定事件依赖的配置参数
     * @return 测试配置
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static BatchProcessingUseCaseConfig of(BatchProcessingUseCaseMarkdownConfig config) {
        return new BatchProcessingUseCaseConfig(config.documentRepository(), config.resultRepository(),
                config.eventRepository(), new InMemoryBatchRepository(), config.extractor(),
                config.markdownPostProcessor(), new InlineExecutorService(), new EmptyCallbackJobRepository());
    }

    /**
     * 使用指定事件和批次依赖创建测试配置。
     *
     * @param config 指定事件依赖的配置参数
     * @return 测试配置
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static BatchProcessingUseCaseConfig of(BatchProcessingUseCaseEventConfig config) {
        return new BatchProcessingUseCaseConfig(config.documentRepository(), new InMemoryOcrResultRepository(),
                config.eventRepository(), config.batchRepository(), config.extractor(), MarkdownPostProcessor.noop(),
                new InlineExecutorService(), config.callbackJobRepository());
    }
}

/**
 * 批次处理 Markdown 测试配置参数。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
record BatchProcessingUseCaseMarkdownConfig(
        InMemoryDocumentJobRepository documentRepository,
        InMemoryOcrResultRepository resultRepository,
        InMemoryOcrEventRepository eventRepository,
        DocumentTextExtractor extractor,
        MarkdownPostProcessor markdownPostProcessor
) {
}

/**
 * 批次处理事件测试配置参数。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
record BatchProcessingUseCaseEventConfig(
        InMemoryDocumentJobRepository documentRepository,
        InMemoryOcrEventRepository eventRepository,
        InMemoryBatchRepository batchRepository,
        DocumentTextExtractor extractor,
        CallbackJobRepository callbackJobRepository
) {

    /**
     * 使用默认空回调仓储创建事件测试配置。
     *
     * @param documentRepository 文档仓储
     * @param eventRepository 事件仓储
     * @param batchRepository 批次仓储
     * @param extractor 文本提取器
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    BatchProcessingUseCaseEventConfig(
            InMemoryDocumentJobRepository documentRepository,
            InMemoryOcrEventRepository eventRepository,
            InMemoryBatchRepository batchRepository,
            DocumentTextExtractor extractor
    ) {
        this(documentRepository, eventRepository, batchRepository, extractor, new EmptyCallbackJobRepository());
    }
}
