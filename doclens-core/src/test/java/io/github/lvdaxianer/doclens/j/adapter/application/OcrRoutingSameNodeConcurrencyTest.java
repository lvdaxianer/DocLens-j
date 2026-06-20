package io.github.lvdaxianer.doclens.j.adapter.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingHitTestFixtures.InMemoryBatchHitTracker;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingTestFixtures.FixedCallIdGenerator;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingTestFixtures.InMemoryCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingTestFixtures.InMemoryRuntimeNodeProvider;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * OCR 路由同节点并发测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
class OcrRoutingSameNodeConcurrencyTest {

    /** 测试 OCR 节点 ID。 */
    private static final String NODE_ID = "node-test";
    /** 测试 OCR 模型标识。 */
    private static final String MODEL_KEY = "stub_ocr";
    /** 负载均衡策略。 */
    private static final String LOAD_BALANCE_STRATEGY = "least-inflight";
    /** 测试批次 ID。 */
    private static final String BATCH_ID = "batch-test";
    /** 测试文档 ID。 */
    private static final String DOCUMENT_ID = "doc-test";
    /** 测试页文件名前缀。 */
    private static final String PAGE_FILE_PREFIX = "page-";
    /** 测试页文件名后缀。 */
    private static final String PAGE_FILE_SUFFIX = ".png";
    /** 测试图片内容前缀。 */
    private static final String IMAGE_CONTENT_PREFIX = "image-";
    /** 测试线程名前缀。 */
    private static final String THREAD_NAME_PREFIX = "doclens-test-ocr-route-concurrent-";
    /** 节点最大并发。 */
    private static final int NODE_MAX_CONCURRENCY = 10;
    /** 测试路由重试次数。 */
    private static final int REQUEST_RETRY_TIMES = 1;
    /** 第一页页码。 */
    private static final int FIRST_PAGE_NO = 1;
    /** 第二页页码。 */
    private static final int SECOND_PAGE_NO = 2;
    /** 并发请求数。 */
    private static final int CONCURRENT_REQUEST_COUNT = 2;
    /** 测试平均延迟毫秒数。 */
    private static final long AVERAGE_LATENCY_MILLIS = 100L;
    /** 测试线程池队列容量。 */
    private static final int TEST_QUEUE_CAPACITY = 4;
    /** 线程保活秒数。 */
    private static final int THREAD_KEEP_ALIVE_SECONDS = 60;
    /** 等待超时秒数。 */
    private static final long AWAIT_TIMEOUT_SECONDS = 2L;
    /** 指定节点失败后是否允许回退。 */
    private static final boolean SPECIFIC_NODE_FALLBACK_ENABLED = false;

    /**
     * 同一 OCR 节点存在可用槽位时应允许两个请求同时执行。
     *
     * @throws ExecutionException Future 执行失败
     * @throws InterruptedException 线程被中断
     * @throws TimeoutException Future 获取超时
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Test
    void routingServiceAllowsTwoRequestsToRunConcurrentlyOnSameNode()
            throws ExecutionException, InterruptedException, TimeoutException {
        BlockingNodeExecutor nodeExecutor = new BlockingNodeExecutor();
        InMemoryRuntimeNodeProvider nodeProvider = new InMemoryRuntimeNodeProvider(List.of(node()));
        OcrRoutingService service = routingService(nodeProvider, nodeExecutor);
        ExecutorService executor = concurrentExecutor();

        try {
            List<Future<OcrRouteExecutionResult>> futures = submitTwoRequests(executor, service);

            assertThat(nodeExecutor.awaitConcurrentCalls()).isTrue();
            assertThat(nodeProvider.inflight(NODE_ID)).isEqualTo(CONCURRENT_REQUEST_COUNT);
            nodeExecutor.releaseCalls();
            assertSuccessfulNodeIds(futures);
        } finally {
            nodeExecutor.releaseCalls();
            executor.shutdownNow();
            executor.awaitTermination(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    /**
     * 创建 OCR 路由服务。
     *
     * @param nodeProvider 运行时节点提供器
     * @param nodeExecutor 节点执行器
     * @return OCR 路由服务
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private OcrRoutingService routingService(
            InMemoryRuntimeNodeProvider nodeProvider,
            OcrNodeImageExecutor nodeExecutor
    ) {
        LeastInflightOcrNodeSelector selector = new LeastInflightOcrNodeSelector();
        return new OcrRoutingService(new OcrRoutingDependencies(nodeProvider, selector,
                new OcrDispatchCoordinator(nodeProvider, selector, new InMemoryPendingQueue()), nodeExecutor,
                new InMemoryCallRepository(), new FixedCallIdGenerator(), new InMemoryBatchHitTracker(),
                new OcrRoutingServiceProperties(OcrRoutePolicy.globalLoadBalance(LOAD_BALANCE_STRATEGY),
                        REQUEST_RETRY_TIMES, SPECIFIC_NODE_FALLBACK_ENABLED)));
    }

    /**
     * 提交两个并发 OCR 请求。
     *
     * @param executor 执行器
     * @param service OCR 路由服务
     * @return 请求 Future
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private List<Future<OcrRouteExecutionResult>> submitTwoRequests(
            ExecutorService executor,
            OcrRoutingService service
    ) {
        List<Future<OcrRouteExecutionResult>> futures = new ArrayList<>(CONCURRENT_REQUEST_COUNT);
        futures.add(executor.submit(() -> service.recognize(request(FIRST_PAGE_NO),
                OcrRoutePolicy.globalLoadBalance(LOAD_BALANCE_STRATEGY))));
        futures.add(executor.submit(() -> service.recognize(request(SECOND_PAGE_NO),
                OcrRoutePolicy.globalLoadBalance(LOAD_BALANCE_STRATEGY))));
        return futures;
    }

    /**
     * 断言两个请求都命中同一节点。
     *
     * @param futures 请求 Future
     * @throws ExecutionException Future 执行失败
     * @throws InterruptedException 线程被中断
     * @throws TimeoutException Future 获取超时
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private void assertSuccessfulNodeIds(List<Future<OcrRouteExecutionResult>> futures)
            throws ExecutionException, InterruptedException, TimeoutException {
        assertThat(futures)
                .extracting(future -> future.get(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS).nodeId())
                .containsExactly(NODE_ID, NODE_ID);
    }

    /**
     * 创建并发测试执行器。
     *
     * @return 并发测试执行器
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private ExecutorService concurrentExecutor() {
        return new ThreadPoolExecutor(CONCURRENT_REQUEST_COUNT, CONCURRENT_REQUEST_COUNT,
                THREAD_KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(TEST_QUEUE_CAPACITY),
                new NamedTestThreadFactory(THREAD_NAME_PREFIX));
    }

    /**
     * 创建图片 OCR 请求。
     *
     * @param pageNo 页码
     * @return 图片 OCR 请求
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private ImageOcrRequest request(int pageNo) {
        String pageFile = PAGE_FILE_PREFIX + pageNo + PAGE_FILE_SUFFIX;
        return new ImageOcrRequest(BATCH_ID, DOCUMENT_ID, pageFile, pageNo,
                (IMAGE_CONTENT_PREFIX + pageNo).getBytes(), JsonPayload.empty());
    }

    /**
     * 创建测试节点。
     *
     * @return OCR 运行时节点
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private OcrRuntimeNodeView node() {
        return new OcrRuntimeNodeView(NODE_ID, MODEL_KEY, true, true, OcrNodeStatus.UP,
                NODE_MAX_CONCURRENCY, 0, AVERAGE_LATENCY_MILLIS);
    }

    /**
     * 阻塞型节点执行器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private static class BlockingNodeExecutor implements OcrNodeImageExecutor {

        private final CountDownLatch enteredCalls = new CountDownLatch(CONCURRENT_REQUEST_COUNT);
        private final CountDownLatch releaseCalls = new CountDownLatch(1);

        /**
         * 记录 OCR 调用并阻塞到测试释放。
         *
         * @param node OCR 节点
         * @param request 图片 OCR 请求
         * @return OCR 图片结果
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        @Override
        public ImageOcrResult recognize(OcrRuntimeNodeView node, ImageOcrRequest request) {
            enteredCalls.countDown();
            awaitRelease();
            return ImageOcrResult.fromBlocks(request.pageNo(), Map.of(), List.of(), List.of());
        }

        /**
         * 等待两个 OCR 调用同时进入。
         *
         * @return 是否等到两个调用
         * @throws InterruptedException 线程被中断
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
                throw new IllegalStateException("interrupted while waiting for OCR release", ex);
            }
        }
    }

    /**
     * 内存待派发队列。
     *
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private static class InMemoryPendingQueue implements OcrPendingRequestQueue {

        private final ArrayDeque<OcrPendingRequest> requests = new ArrayDeque<>(CONCURRENT_REQUEST_COUNT);

        /**
         * 加入待派发请求。
         *
         * @param request 待派发请求
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        @Override
        public void enqueue(OcrPendingRequest request) {
            requests.addLast(request);
        }

        /**
         * 取出最早等待的请求。
         *
         * @return 待派发请求
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        @Override
        public Optional<OcrPendingRequest> poll() {
            return Optional.ofNullable(requests.pollFirst());
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
         * 创建带有测试名前缀的线程。
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
}
