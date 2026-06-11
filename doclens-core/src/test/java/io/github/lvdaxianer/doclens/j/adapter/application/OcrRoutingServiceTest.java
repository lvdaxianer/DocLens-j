package io.github.lvdaxianer.doclens.j.adapter.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingTestFixtures.FixedCallIdGenerator;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingHitTestFixtures.InMemoryBatchHitTracker;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingTestFixtures.InMemoryCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingTestFixtures.InMemoryRuntimeNodeProvider;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingTestFixtures.RecordingNodeExecutor;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * OCR 路由服务测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class OcrRoutingServiceTest {

    /**
     * 单节点失败时应按配置重试 3 次。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void retriesSameNodeThreeTimesBeforeFailing() {
        TestContext context = context(List.of(node("paddle-1", "paddle_ocr")));
        context.executor.fail("paddle-1", 3);

        assertThatThrownBy(() -> context.service.recognize(request(), OcrRoutePolicy.specificNode("paddle_ocr", "paddle-1")))
                .isInstanceOf(OcrRouteExecutionException.class)
                .hasMessageContaining("no healthy ocr candidates");

        assertThat(context.executor.attempts("paddle-1")).isEqualTo(3);
        assertThat(context.nodeProvider.inflight("paddle-1")).isZero();
        assertThat(context.callRepository.calls).hasSize(1);
    }

    /**
     * 全局负载均衡在节点失败耗尽后应切换到其他模型或节点。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void globalModeFailsOverToAnotherHealthyNode() {
        TestContext context = context(List.of(
                node("paddle-1", "paddle_ocr", OcrNodeStatus.UP, 100),
                node("other-1", "other_ocr", OcrNodeStatus.UP, 200)
        ));
        context.executor.fail("paddle-1", 3);

        OcrRouteExecutionResult result = context.service.recognize(request(),
                OcrRoutePolicy.globalLoadBalance("least-inflight"));

        assertThat(result.nodeId()).isEqualTo("other-1");
        assertThat(result.retryCount()).isEqualTo(3);
        assertThat(context.executor.attempts("paddle-1")).isEqualTo(3);
        assertThat(context.executor.attempts("other-1")).isEqualTo(1);
    }

    /**
     * 指定模型负载均衡只能在同模型节点内故障转移。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void modelModeFailsOverOnlyWithinSameModel() {
        TestContext context = context(List.of(
                node("paddle-1", "paddle_ocr"),
                node("paddle-2", "paddle_ocr"),
                node("other-1", "other_ocr")
        ));
        context.executor.fail("paddle-1", 3);

        OcrRouteExecutionResult result = context.service.recognize(request(),
                OcrRoutePolicy.modelLoadBalance("paddle_ocr", "least-inflight"));

        assertThat(result.nodeId()).isEqualTo("paddle-2");
        assertThat(context.executor.attempts("other-1")).isZero();
    }

    /**
     * 指定节点默认失败后不切换到其他节点。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void specificNodeDoesNotFailOverByDefault() {
        TestContext context = context(List.of(node("paddle-1", "paddle_ocr"), node("paddle-2", "paddle_ocr")));
        context.executor.fail("paddle-1", 3);

        assertThatThrownBy(() -> context.service.recognize(request(), OcrRoutePolicy.specificNode("paddle_ocr", "paddle-1")))
                .isInstanceOf(OcrRouteExecutionException.class)
                .hasMessageContaining("no healthy ocr candidates");

        assertThat(context.executor.attempts("paddle-2")).isZero();
    }

    /**
     * 无可用候选节点时应抛出显式 OCR 路由异常。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void allCandidatesFailedThrowsRouteExecutionException() {
        TestContext context = context(List.of(node("down-1", "paddle_ocr", OcrNodeStatus.DOWN)));

        assertThatThrownBy(() -> context.service.recognize(request(),
                OcrRoutePolicy.globalLoadBalance("least-inflight")))
                .isInstanceOf(OcrRouteExecutionException.class)
                .hasMessageContaining("no healthy ocr candidates");
    }

    /**
     * 路由服务应通过协调器路径占用并释放节点槽位。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void routingServiceAcquiresSlotThroughCoordinatorAndReleasesItAfterExecution() {
        TestContext context = context(List.of(node("paddle-1", "paddle_ocr")));

        OcrRouteExecutionResult result = context.service.recognize(request(),
                OcrRoutePolicy.globalLoadBalance("least-inflight"));

        assertThat(result.nodeId()).isEqualTo("paddle-1");
        assertThat(context.nodeProvider.tryAcquireCount()).isEqualTo(1);
        assertThat(context.nodeProvider.releaseCount()).isEqualTo(1);
    }

    /**
     * 创建 OCR 路由测试上下文。
     *
     * @param nodes 运行时节点
     * @return 测试上下文
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private TestContext context(List<OcrRuntimeNodeView> nodes) {
        return context(nodes, new RecordingNodeExecutor());
    }

    /**
     * 创建带自定义执行器的 OCR 路由测试上下文。
     *
     * @param nodes 运行时节点
     * @param executor 节点执行器
     * @return 测试上下文
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private TestContext context(List<OcrRuntimeNodeView> nodes, OcrNodeImageExecutor executor) {
        InMemoryRuntimeNodeProvider nodeProvider = new InMemoryRuntimeNodeProvider(nodes);
        InMemoryCallRepository callRepository = new InMemoryCallRepository();
        InMemoryBatchHitTracker batchHitTracker = new InMemoryBatchHitTracker();
        OcrRoutingService service = service(nodeProvider, executor, callRepository, batchHitTracker);
        return new TestContext(service, nodeProvider, (RecordingNodeExecutor) executor, callRepository, batchHitTracker);
    }

    /**
     * 创建可复用的 OCR 路由服务，避免不同测试路径重复拼装依赖。
     *
     * @param nodeProvider 运行时节点提供器
     * @param executor 节点执行器
     * @param callRepository 调用记录仓储
     * @param batchHitTracker 批次运行时命中跟踪器
     * @return OCR 路由服务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrRoutingService service(
            InMemoryRuntimeNodeProvider nodeProvider,
            OcrNodeImageExecutor executor,
            InMemoryCallRepository callRepository,
            InMemoryBatchHitTracker batchHitTracker
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
                new OcrRoutingServiceProperties(OcrRoutePolicy.globalLoadBalance("least-inflight"), 3, false)
        ));
    }

    /**
     * 创建默认图片 OCR 请求。
     *
     * @return 图片 OCR 请求
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private ImageOcrRequest request() {
        return new ImageOcrRequest("batch-test", "doc-test", "page.png", 1, "image".getBytes(), JsonPayload.empty());
    }

    /**
     * 创建健康运行时节点。
     *
     * @param nodeId 节点 ID
     * @param modelKey 模型标识
     * @return 运行时节点视图
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrRuntimeNodeView node(String nodeId, String modelKey) {
        return node(nodeId, modelKey, OcrNodeStatus.UP);
    }

    /**
     * 创建运行时节点。
     *
     * @param nodeId 节点 ID
     * @param modelKey 模型标识
     * @param status 节点状态
     * @return 运行时节点视图
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrRuntimeNodeView node(String nodeId, String modelKey, OcrNodeStatus status) {
        return node(nodeId, modelKey, status, 100);
    }

    /**
     * 创建运行时节点。
     *
     * @param nodeId 节点 ID
     * @param modelKey 模型标识
     * @param status 节点状态
     * @param avgLatencyMs 平均耗时
     * @return 运行时节点视图
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrRuntimeNodeView node(String nodeId, String modelKey, OcrNodeStatus status, long avgLatencyMs) {
        return new OcrRuntimeNodeView(nodeId, modelKey, true, true, status, 4, 0, avgLatencyMs);
    }

    /**
     * OCR 路由测试上下文。
     *
     * @param service OCR 路由服务
     * @param nodeProvider 节点提供器
     * @param executor 节点执行器
     * @param callRepository 调用记录仓储
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private record TestContext(
            OcrRoutingService service,
            InMemoryRuntimeNodeProvider nodeProvider,
            RecordingNodeExecutor executor,
            InMemoryCallRepository callRepository,
            InMemoryBatchHitTracker batchHitTracker
    ) {
    }

    /**
     * 内存待派发队列。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class InMemoryPendingQueue implements OcrPendingRequestQueue {

        private final java.util.ArrayDeque<OcrPendingRequest> requests = new java.util.ArrayDeque<>(1);

        @Override
        public void enqueue(OcrPendingRequest request) {
            requests.addLast(request);
        }

        @Override
        public java.util.Optional<OcrPendingRequest> poll() {
            return java.util.Optional.ofNullable(requests.pollFirst());
        }
    }
}
