package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRuntimeNodeView;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrBlock;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCreateRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeDeploymentType;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * PaddleOCR 节点图片执行器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class PaddleOcrNodeImageExecutorTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-09T08:00:00+08:00");
    private static final int TEST_TIMEOUT_SECONDS = 5;

    /**
     * 节点 OCR 请求应在线程池隔离的 OCR 请求线程中执行。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void recognizesImageThroughOcrRequestExecutor() {
        OcrNode node = node();
        OcrRuntimeNodePool nodePool = nodePool(node);
        RecordingPaddleOcrNativeClient client = new RecordingPaddleOcrNativeClient();
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable,
                "doclens-ocr-request-test-1"));
        RecordingDashScopeOnlineOcrClient onlineClient = new RecordingDashScopeOnlineOcrClient();
        PaddleOcrNodeImageExecutor nodeExecutor = new PaddleOcrNodeImageExecutor(nodePool, client,
                new PaddleOcrNativeResponseMapper(new ObjectMapper()), onlineClient, executor);

        nodeExecutor.recognize(view(node), request());
        executor.shutdownNow();

        assertThat(client.threadName()).hasValue("doclens-ocr-request-test-1");
        assertThat(onlineClient.called()).isFalse();
    }

    /**
     * 在线节点应走 DashScope compatible 客户端，避免误进入 PaddleOCR 离线路径。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void recognizesOnlineNodeThroughDashScopeClient() {
        OcrNode node = onlineNode();
        OcrRuntimeNodePool nodePool = nodePool(node);
        RecordingPaddleOcrNativeClient paddleClient = new RecordingPaddleOcrNativeClient();
        RecordingDashScopeOnlineOcrClient onlineClient = new RecordingDashScopeOnlineOcrClient();
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable,
                "doclens-ocr-request-online-test-1"));
        PaddleOcrNodeImageExecutor nodeExecutor = new PaddleOcrNodeImageExecutor(nodePool, paddleClient,
                new PaddleOcrNativeResponseMapper(new ObjectMapper()), onlineClient, executor);

        ImageOcrResult result = nodeExecutor.recognize(view(node), request());
        executor.shutdownNow();

        assertThat(paddleClient.threadName()).isEmpty();
        assertThat(onlineClient.threadName()).hasValue("doclens-ocr-request-online-test-1");
        assertThat(result.pageText()).first().extracting("text").isEqualTo("在线识别文本");
    }

    /**
     * 创建 OCR 运行时节点池。
     *
     * @param node OCR 节点
     * @return OCR 运行时节点池
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrRuntimeNodePool nodePool(OcrNode node) {
        OcrRuntimeNodePool nodePool = new OcrRuntimeNodePool(new SingleNodeRepository(node));
        nodePool.initialize();
        return nodePool;
    }

    /**
     * 创建 OCR 节点视图。
     *
     * @param node OCR 节点
     * @return OCR 节点视图
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrRuntimeNodeView view(OcrNode node) {
        return new OcrRuntimeNodeView(node.id(), node.modelKey(), node.enabled(), node.participateGlobal(),
                OcrNodeStatus.UP, node.maxConcurrency(), 0, node.avgLatencyMs());
    }

    /**
     * 创建图片 OCR 请求。
     *
     * @return 图片 OCR 请求
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private ImageOcrRequest request() {
        return new ImageOcrRequest("batch-test", "doc-test", "page.png", 1, "image".getBytes(), JsonPayload.empty());
    }

    /**
     * 创建 OCR 节点。
     *
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNode node() {
        return OcrNode.create(new OcrNodeCreateRequest("node-1", "paddle_ocr", "node-1",
                "127.0.0.1", 8080, true, true, 100, 4, BASE_TIME));
    }

    /**
     * 创建在线 OCR 节点。
     *
     * @return 在线 OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNode onlineNode() {
        return OcrNode.create(new OcrNodeCreateRequest("node-online", "paddle_ocr", OcrNodeDeploymentType.ONLINE,
                "在线节点", "", 0, "aliyun_bailian_dashscope", "qwen-vl-ocr-2025-11-20", "sk-test", true,
                true, true, 100, 4, BASE_TIME));
    }

    /**
     * 生成不发起真实 HTTP 调用的配置。
     *
     * @return DocLens 配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static DocLensProperties properties() {
        DocLensProperties.ThreadPoolProperties pool = new DocLensProperties.ThreadPoolProperties(1, 1, 1, 1,
                "doclens-test-");
        DocLensProperties.ThreadPoolsProperties threadPools = new DocLensProperties.ThreadPoolsProperties(pool, pool,
                pool, pool);
        return new DocLensProperties(
                "target/test-storage",
                true,
                "worker-test",
                new DocLensProperties.CallbackProperties(1, TEST_TIMEOUT_SECONDS),
                new DocLensProperties.AdapterProperties("paddle_ocr"),
                new DocLensProperties.PaddleOcrProperties(true, "http://127.0.0.1:1/ocr", TEST_TIMEOUT_SECONDS, false),
                new DocLensProperties.OcrHealthProperties(3, 2),
                new DocLensProperties.ExtractionProperties(1),
                new DocLensProperties.PdfRenderProperties(144, "png"),
                new DocLensProperties.WordConversionProperties("soffice", TEST_TIMEOUT_SECONDS),
                threadPools
        );
    }

    /**
     * 单节点内存仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private record SingleNodeRepository(OcrNode node) implements OcrNodeRepository {

        @Override
        public void save(OcrNode node) {
            // 测试只读取节点池，无需保存。
        }

        @Override
        public void saveAll(List<OcrNode> nodes) {
            // 测试只读取节点池，无需批量保存。
        }

        @Override
        public void update(OcrNode node) {
            // 测试只读取节点池，无需更新。
        }

        @Override
        public Optional<OcrNode> findById(String nodeId) {
            return node.id().equals(nodeId) ? Optional.of(node) : Optional.empty();
        }

        @Override
        public List<OcrNode> listByModelKey(String modelKey) {
            return node.modelKey().equals(modelKey) ? List.of(node) : List.of();
        }

        @Override
        public List<OcrNode> listEnabled() {
            return List.of(node);
        }

        @Override
        public List<OcrNode> listAll() {
            return List.of(node);
        }

        @Override
        public Optional<OcrNode> findByModelHostPort(String modelKey, String host, int port) {
            return Optional.empty();
        }

        @Override
        public void deleteById(String nodeId) {
            // 测试只读取节点池，无需删除。
        }
    }

    /**
     * 记录调用线程的 PaddleOCR 客户端。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static class RecordingPaddleOcrNativeClient extends PaddleOcrNativeClient {

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        private final AtomicReference<String> threadName = new AtomicReference<>();

        /**
         * 创建记录调用线程的 PaddleOCR 客户端。
         *
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        RecordingPaddleOcrNativeClient() {
            super(properties(), OBJECT_MAPPER);
        }

        @Override
        public JsonNode recognizeImage(OcrRuntimeNode node, byte[] imageContent) {
            threadName.set(Thread.currentThread().getName());
            return OBJECT_MAPPER.createObjectNode().put("errorCode", 0);
        }

        /**
         * 返回记录到的调用线程名。
         *
         * @return 调用线程名
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        Optional<String> threadName() {
            return Optional.ofNullable(threadName.get());
        }
    }

    /**
     * 记录调用线程的 DashScope compatible 在线 OCR 客户端。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static class RecordingDashScopeOnlineOcrClient extends DashScopeOnlineOcrClient {

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        private final AtomicReference<String> threadName = new AtomicReference<>();

        /**
         * 创建记录调用线程的在线 OCR 客户端。
         *
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        RecordingDashScopeOnlineOcrClient() {
            super(OBJECT_MAPPER, URI.create("http://127.0.0.1:1/compatible-mode/v1/chat/completions"),
                    Duration.ofSeconds(TEST_TIMEOUT_SECONDS));
        }

        @Override
        public ImageOcrResult recognizeImage(OcrRuntimeNode node, ImageOcrRequest request) {
            threadName.set(Thread.currentThread().getName());
            return ImageOcrResult.fromBlocks(request.pageNo(), Map.of(),
                    List.of(new OcrBlock(request.pageNo(), "在线识别文本", 1D, List.of(), List.of(), "test")),
                    List.of());
        }

        /**
         * 返回是否被调用。
         *
         * @return 是否被调用
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        boolean called() {
            return threadName.get() != null;
        }

        /**
         * 返回记录到的调用线程名。
         *
         * @return 调用线程名
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        Optional<String> threadName() {
            return Optional.ofNullable(threadName.get());
        }
    }
}
