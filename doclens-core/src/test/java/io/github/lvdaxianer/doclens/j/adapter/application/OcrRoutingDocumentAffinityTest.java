package io.github.lvdaxianer.doclens.j.adapter.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingHitTestFixtures.InMemoryBatchHitTracker;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingTestFixtures.FixedCallIdGenerator;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingTestFixtures.InMemoryCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingTestFixtures.InMemoryRuntimeNodeProvider;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingTestFixtures.RecordingNodeExecutor;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * OCR 路由文档级模型亲和力测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
class OcrRoutingDocumentAffinityTest {

    private static final String BATCH_ID = "batch-affinity";
    private static final String DOCUMENT_ID = "doc-affinity";
    private static final String PADDLE_MODEL_KEY = "paddle_ocr";
    private static final String OLLAMA_MODEL_KEY = "ollama_deepseek_ocr";
    private static final String PADDLE_NODE_ONE = "paddle-1";
    private static final String PADDLE_NODE_TWO = "paddle-2";
    private static final String OLLAMA_NODE_ONE = "ollama-1";
    private static final String LOAD_BALANCE_STRATEGY = "least-inflight";
    private static final int REQUEST_RETRY_TIMES = 3;
    private static final int FIRST_PAGE_NO = 1;
    private static final int SECOND_PAGE_NO = 2;

    /**
     * 同一文档首次绑定模型后，后续页只能在同模型节点内路由。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void keepsLaterPagesWithinBoundDocumentModel() {
        TestContext context = context(List.of(
                node(PADDLE_NODE_ONE, PADDLE_MODEL_KEY, 1),
                node(PADDLE_NODE_TWO, PADDLE_MODEL_KEY, 100),
                node(OLLAMA_NODE_ONE, OLLAMA_MODEL_KEY, 200)
        ));

        OcrRouteExecutionResult firstResult = context.service().recognize(request(FIRST_PAGE_NO),
                OcrRoutePolicy.modelLoadBalance(PADDLE_MODEL_KEY, LOAD_BALANCE_STRATEGY));
        OcrRouteExecutionResult secondResult = context.service().recognize(request(SECOND_PAGE_NO),
                OcrRoutePolicy.globalLoadBalance(LOAD_BALANCE_STRATEGY));

        assertThat(firstResult.modelKey()).isEqualTo(PADDLE_MODEL_KEY);
        assertThat(secondResult.modelKey()).isEqualTo(PADDLE_MODEL_KEY);
        assertThat(secondResult.nodeId()).isIn(PADDLE_NODE_ONE, PADDLE_NODE_TWO);
        assertThat(context.executor().attempts(OLLAMA_NODE_ONE)).isZero();
    }

    /**
     * 已绑定模型全部失败时，同一文档不能跨模型故障转移。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void doesNotFailOverToAnotherModelAfterDocumentModelBound() {
        TestContext context = context(List.of(
                node(PADDLE_NODE_ONE, PADDLE_MODEL_KEY, 10),
                node(PADDLE_NODE_TWO, PADDLE_MODEL_KEY, 20),
                node(OLLAMA_NODE_ONE, OLLAMA_MODEL_KEY, 1)
        ));

        context.service().recognize(request(FIRST_PAGE_NO),
                OcrRoutePolicy.modelLoadBalance(PADDLE_MODEL_KEY, LOAD_BALANCE_STRATEGY));
        context.executor().fail(PADDLE_NODE_ONE, REQUEST_RETRY_TIMES + FIRST_PAGE_NO);
        context.executor().fail(PADDLE_NODE_TWO, REQUEST_RETRY_TIMES);

        assertThatThrownBy(() -> context.service().recognize(request(SECOND_PAGE_NO),
                OcrRoutePolicy.globalLoadBalance(LOAD_BALANCE_STRATEGY)))
                .isInstanceOf(OcrRouteExecutionException.class)
                .hasMessageContaining("no healthy ocr candidates");
        assertThat(context.executor().attempts(OLLAMA_NODE_ONE)).isZero();
    }

    /**
     * 创建 OCR 路由测试上下文。
     *
     * @param nodes 运行时节点
     * @return 测试上下文
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private TestContext context(List<OcrRuntimeNodeView> nodes) {
        InMemoryRuntimeNodeProvider nodeProvider = new InMemoryRuntimeNodeProvider(nodes);
        RecordingNodeExecutor executor = new RecordingNodeExecutor();
        LeastInflightOcrNodeSelector selector = new LeastInflightOcrNodeSelector();
        OcrRoutingService service = new OcrRoutingService(new OcrRoutingDependencies(
                nodeProvider,
                selector,
                new OcrDispatchCoordinator(nodeProvider, selector, new InMemoryPendingQueue()),
                executor,
                new InMemoryCallRepository(),
                new FixedCallIdGenerator(),
                new InMemoryBatchHitTracker(),
                new OcrRoutingServiceProperties(OcrRoutePolicy.globalLoadBalance(LOAD_BALANCE_STRATEGY),
                        REQUEST_RETRY_TIMES, false)
        ));
        return new TestContext(service, executor);
    }

    /**
     * 创建图片 OCR 请求。
     *
     * @param pageNo 页码
     * @return 图片 OCR 请求
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private ImageOcrRequest request(int pageNo) {
        return new ImageOcrRequest(BATCH_ID, DOCUMENT_ID, "page-" + pageNo + ".png", pageNo,
                "image".getBytes(), JsonPayload.empty());
    }

    /**
     * 创建运行时节点。
     *
     * @param nodeId 节点 ID
     * @param modelKey 模型标识
     * @param avgLatencyMs 平均耗时
     * @return 运行时节点
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private OcrRuntimeNodeView node(String nodeId, String modelKey, long avgLatencyMs) {
        return new OcrRuntimeNodeView(nodeId, modelKey, true, true, OcrNodeStatus.UP, 4, 0, avgLatencyMs);
    }

    /**
     * OCR 路由测试上下文。
     *
     * @param service OCR 路由服务
     * @param executor 记录型执行器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private record TestContext(OcrRoutingService service, RecordingNodeExecutor executor) {
    }

    /**
     * 内存待派发队列。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private static final class InMemoryPendingQueue implements OcrPendingRequestQueue {

        private final ArrayDeque<OcrPendingRequest> requests = new ArrayDeque<>(1);

        @Override
        public void enqueue(OcrPendingRequest request) {
            requests.addLast(request);
        }

        @Override
        public Optional<OcrPendingRequest> poll() {
            return Optional.ofNullable(requests.pollFirst());
        }
    }
}
