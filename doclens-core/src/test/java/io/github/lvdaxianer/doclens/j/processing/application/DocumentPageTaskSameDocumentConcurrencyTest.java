package io.github.lvdaxianer.doclens.j.processing.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrRouteExecutionResult;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingService;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrBlock;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTask;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * 同一文档页任务并发执行测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
class DocumentPageTaskSameDocumentConcurrencyTest {

    /** 批次 ID。 */
    private static final String BATCH_ID = "batch-test";
    /** 文档 ID。 */
    private static final String DOCUMENT_ID = "doc-test";
    /** 第一个任务 ID。 */
    private static final String FIRST_TASK_ID = "task-1";
    /** 第二个任务 ID。 */
    private static final String SECOND_TASK_ID = "task-2";
    /** 文档文件名。 */
    private static final String FILE_NAME = "doc-test.pdf";
    /** 存储 URI 前缀。 */
    private static final String STORAGE_URI_PREFIX = "local://";
    /** 页文件路径片段。 */
    private static final String PAGE_PATH_SEGMENT = "/page-";
    /** 页文本前缀。 */
    private static final String PAGE_TEXT_PREFIX = "page-";
    /** OCR 模型标识。 */
    private static final String OCR_MODEL_KEY = "stub_ocr";
    /** OCR 节点 ID。 */
    private static final String OCR_NODE_ID = "node-test";
    /** 测试工作线程 ID。 */
    private static final String WORKER_ID = "worker-test";
    /** 线程名前缀。 */
    private static final String THREAD_NAME_PREFIX = "doclens-test-page-task-";
    /** 并发页任务数量。 */
    private static final int CONCURRENT_PAGE_COUNT = 2;
    /** 第一页页码。 */
    private static final int FIRST_PAGE_NO = 1;
    /** 第二页页码。 */
    private static final int SECOND_PAGE_NO = 2;
    /** 锁定秒数。 */
    private static final int LOCK_SECONDS = 30;
    /** 文档大小。 */
    private static final long FILE_SIZE = 100L;
    /** 文档页数。 */
    private static final int PAGE_COUNT = 2;
    /** 上传排序。 */
    private static final int SORT_ORDER = 0;
    /** OCR 重试次数。 */
    private static final int RETRY_COUNT = 0;
    /** 路由耗时毫秒数。 */
    private static final long ROUTE_ELAPSED_MILLIS = 1L;
    /** 测试置信度。 */
    private static final double TEST_CONFIDENCE = 0.99D;
    /** 等待超时秒数。 */
    private static final long AWAIT_TIMEOUT_SECONDS = 2L;

    /**
     * 同一文档的两个页任务应能在一轮扫描内同时进入 OCR。
     *
     * @throws InterruptedException 线程等待被中断
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Test
    void runOnceSubmitsTwoSameDocumentPagesToOcrConcurrently() throws InterruptedException {
        InMemoryDocumentPageTaskRepository taskRepository = new InMemoryDocumentPageTaskRepository();
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        BlockingOcrRoutingService routingService = new BlockingOcrRoutingService();
        ExecutorService executor = concurrentExecutor();
        seedTasks(taskRepository, documentRepository);
        PageTaskExecutionTestContext context = new PageTaskExecutionTestContext(taskRepository, documentRepository,
                routingService, executor);
        DocumentPageTaskExecutionService service = service(context);

        try {
            assertThat(service.runOnce()).isEqualTo(CONCURRENT_PAGE_COUNT);
            assertThat(routingService.awaitConcurrentCalls()).isTrue();
            assertThat(taskRepository.listByDocumentId(DOCUMENT_ID))
                    .extracting(DocumentPageTask::status)
                    .containsOnly(DocumentPageTaskStatus.PROCESSING);
        } finally {
            routingService.releaseCalls();
            executor.shutdownNow();
            executor.awaitTermination(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    /**
     * 创建页任务执行服务。
     *
     * @param context 测试上下文
     * @return 页任务执行服务
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private DocumentPageTaskExecutionService service(PageTaskExecutionTestContext context) {
        DocumentPageTaskExecutionDependencies dependencies = new DocumentPageTaskExecutionDependencies(
                context.taskRepository(), context.documentRepository(),
                new DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository(),
                new DocumentPageTaskExecutionTestDoubles.RecordingObjectStorage(), context.routingService());
        return new DocumentPageTaskExecutionService(dependencies,
                new DocumentPageTaskExecutionTestDoubles.InlineTransactionRunner(),
                new DocumentPageTaskExecutionOptions(WORKER_ID, CONCURRENT_PAGE_COUNT, LOCK_SECONDS,
                        context.executor(),
                        task -> {
                            // 本测试只验证页任务能同时进入 OCR，聚合监听由独立测试覆盖。
                        }));
    }

    /**
     * 初始化文档和页任务。
     *
     * @param taskRepository 页任务仓储
     * @param documentRepository 文档仓储
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private void seedTasks(
            InMemoryDocumentPageTaskRepository taskRepository,
            InMemoryDocumentJobRepository documentRepository
    ) {
        documentRepository.save(document());
        taskRepository.saveAll(List.of(task(FIRST_TASK_ID, FIRST_PAGE_NO), task(SECOND_TASK_ID, SECOND_PAGE_NO)));
    }

    /**
     * 创建测试文档。
     *
     * @return 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private DocumentJob document() {
        DocumentJobCreateRequest request = new DocumentJobCreateRequest(DOCUMENT_ID, BATCH_ID, FILE_NAME,
                DocumentType.PDF, FILE_SIZE, PAGE_COUNT, STORAGE_URI_PREFIX + DOCUMENT_ID, OCR_MODEL_KEY,
                Optional.empty(), JsonPayload.empty(), SORT_ORDER, OffsetDateTime.now());
        return DocumentJob.create(request).markOcrQueued(CONCURRENT_PAGE_COUNT, OffsetDateTime.now());
    }

    /**
     * 创建等待 OCR 的页任务。
     *
     * @param taskId 任务 ID
     * @param pageNo 页码
     * @return 页任务
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private DocumentPageTask task(String taskId, int pageNo) {
        DocumentPageTaskCreateRequest request = new DocumentPageTaskCreateRequest(taskId, BATCH_ID, DOCUMENT_ID,
                pageNo, STORAGE_URI_PREFIX + DOCUMENT_ID + PAGE_PATH_SEGMENT + pageNo, OffsetDateTime.now());
        return DocumentPageTask.create(request);
    }

    /**
     * 创建并发测试执行器。
     *
     * @return 并发测试执行器
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private ExecutorService concurrentExecutor() {
        return new ThreadPoolExecutor(CONCURRENT_PAGE_COUNT, CONCURRENT_PAGE_COUNT,
                AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(CONCURRENT_PAGE_COUNT),
                new NamedTestThreadFactory(THREAD_NAME_PREFIX));
    }

    /**
     * 阻塞型 OCR 路由服务。
     *
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private static class BlockingOcrRoutingService extends OcrRoutingService {

        private final CountDownLatch enteredCalls = new CountDownLatch(CONCURRENT_PAGE_COUNT);
        private final CountDownLatch releaseCalls = new CountDownLatch(1);

        /**
         * 创建阻塞型 OCR 路由服务。
         *
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        BlockingOcrRoutingService() {
            super(DocumentPageTaskRoutingTestDoubles.routingDependencies());
        }

        /**
         * 阻塞到测试释放并返回固定 OCR 结果。
         *
         * @param request 图片 OCR 请求
         * @param requestedPolicy 请求路由策略
         * @return OCR 路由结果
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        @Override
        public OcrRouteExecutionResult recognize(ImageOcrRequest request, OcrRoutePolicy requestedPolicy) {
            enteredCalls.countDown();
            awaitRelease();
            ImageOcrResult result = ImageOcrResult.fromBlocks(request.pageNo(), Map.of(),
                    List.of(new OcrBlock(request.pageNo(), PAGE_TEXT_PREFIX + request.pageNo(), TEST_CONFIDENCE,
                            List.of(), List.of(), FILE_NAME)), List.of());
            return new OcrRouteExecutionResult(result, OCR_MODEL_KEY, OCR_NODE_ID, ROUTE_ELAPSED_MILLIS,
                    RETRY_COUNT);
        }

        /**
         * 等待两个页任务都进入 OCR。
         *
         * @return 是否等到两个 OCR 调用
         * @throws InterruptedException 线程等待被中断
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        boolean awaitConcurrentCalls() throws InterruptedException {
            return enteredCalls.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        /**
         * 释放阻塞中的 OCR 调用。
         *
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        void releaseCalls() {
            releaseCalls.countDown();
        }

        /**
         * 等待测试释放 OCR 调用。
         *
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        private void awaitRelease() {
            try {
                releaseCalls.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while waiting for page OCR release", ex);
            }
        }
    }

    /**
     * 测试命名线程工厂。
     *
     * @param threadNamePrefix 线程名前缀
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private record NamedTestThreadFactory(String threadNamePrefix) implements ThreadFactory {

        private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

        /**
         * 创建带测试名前缀的线程。
         *
         * @param runnable 线程任务
         * @return 命名后的线程
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, threadNamePrefix + THREAD_COUNTER.incrementAndGet());
        }
    }

    /**
     * 页任务执行测试上下文。
     *
     * @param taskRepository 页任务仓储
     * @param documentRepository 文档仓储
     * @param routingService OCR 路由服务
     * @param executor 页任务执行器
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private record PageTaskExecutionTestContext(
            InMemoryDocumentPageTaskRepository taskRepository,
            InMemoryDocumentJobRepository documentRepository,
            OcrRoutingService routingService,
            ExecutorService executor
    ) {
    }
}
