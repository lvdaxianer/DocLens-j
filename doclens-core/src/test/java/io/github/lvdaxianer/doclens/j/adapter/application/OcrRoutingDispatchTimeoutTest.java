package io.github.lvdaxianer.doclens.j.adapter.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingHitTestFixtures.InMemoryBatchHitTracker;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingTestFixtures.FixedCallIdGenerator;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingTestFixtures.InMemoryCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingTestFixtures.InMemoryRuntimeNodeProvider;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingTestFixtures.RecordingNodeExecutor;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrHealthGovernance;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * OCR 路由派发等待超时测试。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-12
 */
class OcrRoutingDispatchTimeoutTest {

    /**
     * 排队派发超过配置等待时间时应稳定失败。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    @Test
    void queuedDispatchFailsWithStableTimeoutWhenNoSlotBecomesAvailable() {
        OcrRoutingService service = service(Duration.ofMillis(10));

        assertTimeoutPreemptively(Duration.ofMillis(300), () ->
                assertThatThrownBy(() -> service.recognize(request(),
                        OcrRoutePolicy.globalLoadBalance("least-inflight")))
                        .isInstanceOf(OcrRouteExecutionException.class)
                        .hasMessageContaining("ocr dispatch wait timed out"));
    }

    /**
     * 创建指定等待超时的 OCR 路由服务。
     *
     * @param dispatchWaitTimeout 派发等待超时
     * @return OCR 路由服务
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    private OcrRoutingService service(Duration dispatchWaitTimeout) {
        LeastInflightOcrNodeSelector selector = new LeastInflightOcrNodeSelector();
        InMemoryRuntimeNodeProvider nodeProvider = new InMemoryRuntimeNodeProvider(
                List.of(fullNode("paddle-1", "paddle_ocr")));
        return new OcrRoutingService(new OcrRoutingDependencies(nodeProvider, selector,
                new OcrDispatchCoordinator(nodeProvider, selector, new InMemoryPendingQueue()),
                new RecordingNodeExecutor(), new InMemoryCallRepository(), new FixedCallIdGenerator(),
                new InMemoryBatchHitTracker(), timeoutProperties(dispatchWaitTimeout)));
    }

    /**
     * 创建指定等待超时的 OCR 路由配置。
     *
     * @param dispatchWaitTimeout 派发等待超时
     * @return OCR 路由服务配置
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    private OcrRoutingServiceProperties timeoutProperties(Duration dispatchWaitTimeout) {
        return new OcrRoutingServiceProperties(OcrRoutePolicy.globalLoadBalance("least-inflight"),
                OcrRoutingServiceProperties.DEFAULT_IDLE_FACTOR, OcrRoutingServiceProperties.DEFAULT_WEIGHT_FACTOR,
                OcrRoutingServiceProperties.DEFAULT_TOP_BUCKET_THRESHOLD, 3,
                OcrHealthGovernance.defaults(), false, dispatchWaitTimeout);
    }

    /**
     * 创建默认图片 OCR 请求。
     *
     * @return 图片 OCR 请求
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    private ImageOcrRequest request() {
        return new ImageOcrRequest("batch-test", "doc-test", "page.png", 1, "image".getBytes(), JsonPayload.empty());
    }

    /**
     * 创建无可用槽位的运行时节点。
     *
     * @param nodeId 节点 ID
     * @param modelKey 模型标识
     * @return 满载运行时节点视图
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    private OcrRuntimeNodeView fullNode(String nodeId, String modelKey) {
        return new OcrRuntimeNodeView(nodeId, modelKey, true, true, OcrNodeStatus.UP, 50, 1, 1, 0, 0,
                100L, Optional.empty(), 0L, 0L);
    }

    /**
     * 内存待派发队列。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    private static final class InMemoryPendingQueue implements OcrPendingRequestQueue {

        private final ArrayDeque<OcrPendingRequest> requests = new ArrayDeque<>(1);

        @Override
        public boolean enqueue(OcrPendingRequest request) {
            requests.addLast(request);
            return true;
        }

        @Override
        public Optional<OcrPendingRequest> poll() {
            return Optional.ofNullable(requests.pollFirst());
        }
    }
}
