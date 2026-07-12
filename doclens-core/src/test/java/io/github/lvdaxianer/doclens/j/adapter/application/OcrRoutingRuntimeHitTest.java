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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * OCR 路由运行时命中节点测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class OcrRoutingRuntimeHitTest {

    /*
     * 运行时命中跟踪需要用阻塞执行器观察“请求尚未完成”的中间态。
     * 这个并发场景与普通路由重试/故障转移不同，单独放在本类，
     * 可以避免 OcrRoutingServiceTest 同时承担同步路由和运行态快照职责。
     */

    /**
     * 路由执行期间应暴露运行时命中节点，便于 Dashboard 在处理中展示真实分布。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void routingServiceTracksBatchHitNodeWhileRequestIsStillRunning() throws Exception {
        RuntimeHitContext context = runtimeHitContext();
        AtomicReference<OcrRouteExecutionResult> resultRef = new AtomicReference<>();
        Thread worker = workerThread(context.service(), resultRef);

        worker.start();
        assertThat(context.executor().awaitStarted()).isTrue();

        assertRuntimeHitVisible(context);
        context.executor().release();
        worker.join(TimeUnit.SECONDS.toMillis(2));

        assertThat(resultRef.get()).isNotNull();
        assertThat(context.batchHitTracker().snapshotByBatch("batch-test")).isEmpty();
        assertThat(context.callRepository().calls).hasSize(1);
    }

    /**
     * 路由执行期间应把已派发节点同步到图片页任务快照。
     *
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Test
    void routingServicePublishesPageTaskAssignmentWhileRequestIsStillRunning() throws Exception {
        RuntimeHitContext context = runtimeHitContext();
        OcrRunningPageTaskCommand command = new OcrRunningPageTaskCommand("task-1", "batch-test",
                "doc-test", 1, "worker-a", "doclens-page-task-ocr-1", java.time.OffsetDateTime.now());
        context.runningPageTaskTracker().recordStart(command);
        AtomicReference<OcrRouteExecutionResult> resultRef = new AtomicReference<>();
        Thread worker = workerThread(context.service(), resultRef);

        worker.start();
        assertThat(context.executor().awaitStarted()).isTrue();

        assertThat(context.runningPageTaskTracker().snapshotByBatch("batch-test"))
                .singleElement()
                .satisfies(task -> {
                    assertThat(task.modelKey()).contains("paddle_ocr");
                    assertThat(task.nodeId()).contains("paddle-1");
                });
        context.executor().release();
        worker.join(TimeUnit.SECONDS.toMillis(2));
    }

    /**
     * 创建运行时命中测试上下文。
     *
     * @return 运行时命中测试上下文
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private RuntimeHitContext runtimeHitContext() {
        BlockingNodeExecutor executor = new BlockingNodeExecutor();
        InMemoryRuntimeNodeProvider nodeProvider = new InMemoryRuntimeNodeProvider(List.of(node()));
        InMemoryCallRepository callRepository = new InMemoryCallRepository();
        InMemoryBatchHitTracker batchHitTracker = new InMemoryBatchHitTracker();
        InMemoryOcrRunningPageTaskTracker runningPageTaskTracker = new InMemoryOcrRunningPageTaskTracker();
        OcrRoutingService service = service(nodeProvider, executor, callRepository, batchHitTracker,
                runningPageTaskTracker);
        return new RuntimeHitContext(service, executor, callRepository, batchHitTracker, runningPageTaskTracker);
    }

    /**
     * 创建执行 OCR 请求的工作线程。
     *
     * @param service OCR 路由服务
     * @param resultRef 结果引用
     * @return 工作线程
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private Thread workerThread(OcrRoutingService service, AtomicReference<OcrRouteExecutionResult> resultRef) {
        return new Thread(() -> resultRef.set(service.recognize(request(),
                OcrRoutePolicy.globalLoadBalance("least-inflight"))));
    }

    /**
     * 校验请求执行中的运行时命中节点快照。
     *
     * @param context 运行时命中测试上下文
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertRuntimeHitVisible(RuntimeHitContext context) {
        assertThat(context.batchHitTracker().snapshotByBatch("batch-test")).singleElement().satisfies(hit ->
                assertThat(Map.of(
                        "modelKey", hit.modelKey(),
                        "nodeId", hit.nodeId(),
                        "imageCount", hit.imageCount()))
                        .containsEntry("modelKey", "paddle_ocr")
                        .containsEntry("nodeId", "paddle-1")
                        .containsEntry("imageCount", 1L));
        assertThat(context.callRepository().calls).isEmpty();
    }

    /**
     * 创建可复用的 OCR 路由服务。
     *
     * @param nodeProvider 运行时节点提供器
     * @param executor 节点执行器
     * @param callRepository 调用记录仓储
     * @param batchHitTracker 批次运行时命中跟踪器
     * @return OCR 路由服务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private OcrRoutingService service(
            InMemoryRuntimeNodeProvider nodeProvider,
            OcrNodeImageExecutor executor,
            InMemoryCallRepository callRepository,
            InMemoryBatchHitTracker batchHitTracker,
            OcrRunningPageTaskTracker runningPageTaskTracker
    ) {
        LeastInflightOcrNodeSelector selector = new LeastInflightOcrNodeSelector();
        return new OcrRoutingService(new OcrRoutingDependencies(
                nodeProvider,
                selector,
                new OcrDispatchCoordinator(nodeProvider, selector, new InMemoryPendingQueue()),
                executor,
                callRepository,
                new FixedCallIdGenerator(),
                batchHitTracker,
                new OcrRoutingServiceProperties(OcrRoutePolicy.globalLoadBalance("least-inflight"), 3, false),
                new InMemoryOcrDocumentAffinityTracker(),
                runningPageTaskTracker
        ));
    }

    /**
     * 创建默认图片 OCR 请求。
     *
     * @return 图片 OCR 请求
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private ImageOcrRequest request() {
        return new ImageOcrRequest("batch-test", "task-1", "doc-test", "page.png", 1,
                "image".getBytes(), JsonPayload.empty());
    }

    /**
     * 创建健康运行时节点。
     *
     * @return 运行时节点视图
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private OcrRuntimeNodeView node() {
        return new OcrRuntimeNodeView("paddle-1", "paddle_ocr", true, true, OcrNodeStatus.UP, 4, 0, 100);
    }

    /**
     * OCR 路由运行时命中测试上下文。
     *
     * @param service OCR 路由服务
     * @param executor 阻塞执行器
     * @param callRepository 调用记录仓储
     * @param batchHitTracker 批次命中跟踪器
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private record RuntimeHitContext(
            OcrRoutingService service,
            BlockingNodeExecutor executor,
            InMemoryCallRepository callRepository,
            InMemoryBatchHitTracker batchHitTracker,
            OcrRunningPageTaskTracker runningPageTaskTracker
    ) {
    }

    /**
     * 阻塞型执行器，用于观察 OCR 调用进行中的运行时状态。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static final class BlockingNodeExecutor implements OcrNodeImageExecutor {

        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        @Override
        public ImageOcrResult recognize(OcrRuntimeNodeView node, ImageOcrRequest request) {
            started.countDown();
            awaitRelease();
            return ImageOcrResult.fromBlocks(request.pageNo(), Map.of(), List.of(), List.of());
        }

        /**
         * 等待执行器开始处理。
         *
         * @return 是否在超时前开始处理
         * @throws InterruptedException 线程中断
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        boolean awaitStarted() throws InterruptedException {
            return started.await(2, TimeUnit.SECONDS);
        }

        /**
         * 释放被阻塞的执行器。
         *
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        void release() {
            release.countDown();
        }

        /**
         * 等待外部放行，避免测试线程忙等。
         *
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        private void awaitRelease() {
            try {
                release.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("blocking executor interrupted", ex);
            }
        }
    }

    /**
     * 内存待派发队列。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static final class InMemoryPendingQueue implements OcrPendingRequestQueue {

        private final java.util.ArrayDeque<OcrPendingRequest> requests = new java.util.ArrayDeque<>(1);

        @Override
        public boolean enqueue(OcrPendingRequest request) {
            requests.addLast(request);
            return true;
        }

        @Override
        public java.util.Optional<OcrPendingRequest> poll() {
            return java.util.Optional.ofNullable(requests.pollFirst());
        }
    }
}
