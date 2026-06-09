package io.github.lvdaxianer.doclens.j.processing.infrastructure.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.application.LeastInflightOcrNodeSelector;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrCallIdGenerator;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeImageExecutor;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRouteExecutionResult;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingDependencies;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingService;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingServiceProperties;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRuntimeNodeProvider;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRuntimeNodeView;
import io.github.lvdaxianer.doclens.j.adapter.domain.DefaultAdapterRegistry;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrAdapter;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrBlock;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCall;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutingMode;
import io.github.lvdaxianer.doclens.j.api.AdapterCapability;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionRequest;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionResult;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 图片文档 OCR 提取器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class ImageDocumentExtractorTest {

    private static final String ADAPTER_KEY = "paddle_ocr";
    private static final String MODEL_KEY = "paddle_ocr";
    private static final String NODE_ID = "paddle-node-1";

    /**
     * 图片提取应使用文档路由策略命中指定 OCR 节点。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void extractUsesDocumentRoutePolicyThroughRoutingService() {
        InMemoryNodeProvider nodeProvider = new InMemoryNodeProvider();
        RecordingNodeExecutor nodeExecutor = new RecordingNodeExecutor();
        InMemoryCallRepository callRepository = new InMemoryCallRepository();
        OcrRoutingService routingService = routingService(nodeProvider, nodeExecutor, callRepository);
        ImageDocumentExtractor extractor = new ImageDocumentExtractor(adapterRegistry(), routingService);

        DocumentTextExtractionResult result = extractor.extract(extractionRequest());

        assertThat(result.finalText()).isEqualTo("routed text");
        assertThat(nodeExecutor.requests).hasSize(1);
        assertThat(nodeExecutor.requests.getFirst().fileName()).isEqualTo("route.png");
        assertThat(callRepository.calls).hasSize(1);
        assertThat(callRepository.calls.getFirst().routingMode()).isEqualTo(OcrRoutingMode.SPECIFIC_NODE);
        assertThat(callRepository.calls.getFirst().nodeId()).isEqualTo(NODE_ID);
    }

    /**
     * 创建 OCR 路由服务。
     *
     * @param nodeProvider 运行时节点提供器
     * @param nodeExecutor 节点执行器
     * @param callRepository 调用记录仓储
     * @return OCR 路由服务
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrRoutingService routingService(
            InMemoryNodeProvider nodeProvider,
            RecordingNodeExecutor nodeExecutor,
            InMemoryCallRepository callRepository
    ) {
        return new OcrRoutingService(new OcrRoutingDependencies(nodeProvider, new LeastInflightOcrNodeSelector(),
                nodeExecutor, callRepository, new FixedCallIdGenerator(),
                new OcrRoutingServiceProperties(OcrRoutePolicy.globalLoadBalance("least-inflight"), 3, false)));
    }

    /**
     * 创建提取请求。
     *
     * @return 文档提取请求
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private DocumentTextExtractionRequest extractionRequest() {
        return new DocumentTextExtractionRequest(document(), "image".getBytes(), ADAPTER_KEY);
    }

    /**
     * 创建携带指定节点策略的文档任务。
     *
     * @return 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private DocumentJob document() {
        return DocumentJob.create(new DocumentJobCreateRequest("doc-1", "batch-1", "route.png",
                DocumentType.IMAGE, 5, 1, "memory://route.png", ADAPTER_KEY, Optional.empty(),
                OcrRoutePolicy.specificNode(MODEL_KEY, NODE_ID), JsonPayload.empty(), 0, OffsetDateTime.now()));
    }

    /**
     * 创建旧适配器注册表。
     *
     * @return 适配器注册表
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private DefaultAdapterRegistry adapterRegistry() {
        return new DefaultAdapterRegistry(List.of(new LegacyFailingAdapter()));
    }

    /**
     * 内存运行时节点提供器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static class InMemoryNodeProvider implements OcrRuntimeNodeProvider {

        private int inflightImages;

        @Override
        public List<OcrRuntimeNodeView> snapshot() {
            return List.of(new OcrRuntimeNodeView(NODE_ID, MODEL_KEY, true, true, OcrNodeStatus.UP, 4,
                    inflightImages, 100));
        }

        @Override
        public Optional<OcrRuntimeNodeView> incrementInflight(String nodeId) {
            inflightImages++;
            return snapshot().stream().filter(node -> node.nodeId().equals(nodeId)).findFirst();
        }

        @Override
        public Optional<OcrRuntimeNodeView> decrementInflight(String nodeId) {
            inflightImages = Math.max(0, inflightImages - 1);
            return snapshot().stream().filter(node -> node.nodeId().equals(nodeId)).findFirst();
        }

        @Override
        public Optional<OcrRuntimeNodeView> tryAcquireSlot(String nodeId) {
            return incrementInflight(nodeId);
        }

        @Override
        public Optional<OcrRuntimeNodeView> releaseSlot(String nodeId) {
            return decrementInflight(nodeId);
        }

        @Override
        public Optional<OcrRuntimeNodeView> incrementQueued(String nodeId) {
            return snapshot().stream().filter(node -> node.nodeId().equals(nodeId)).findFirst();
        }

        @Override
        public Optional<OcrRuntimeNodeView> decrementQueued(String nodeId) {
            return snapshot().stream().filter(node -> node.nodeId().equals(nodeId)).findFirst();
        }
    }

    /**
     * 记录请求的节点执行器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static class RecordingNodeExecutor implements OcrNodeImageExecutor {

        private final List<ImageOcrRequest> requests = new ArrayList<>(1);

        @Override
        public ImageOcrResult recognize(OcrRuntimeNodeView node, ImageOcrRequest request) {
            requests.add(request);
            OcrBlock block = new OcrBlock(1, "routed text", 0.99D, List.of(), List.of(), "test");
            return ImageOcrResult.fromBlocks(1, Map.of("nodeId", node.nodeId()), List.of(block), List.of());
        }
    }

    /**
     * 内存调用记录仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static class InMemoryCallRepository implements OcrNodeCallRepository {

        private final List<OcrNodeCall> calls = new ArrayList<>(1);

        @Override
        public void save(OcrNodeCall call) {
            calls.add(call);
        }

        @Override
        public List<OcrNodeCall> listByDocumentId(String documentId) {
            return calls.stream().filter(call -> call.documentId().equals(documentId)).toList();
        }

        @Override
        public List<OcrNodeCall> listByBatchId(String batchId) {
            return calls.stream().filter(call -> call.batchId().equals(batchId)).toList();
        }

        @Override
        public List<OcrNodeCall> listRecentByNodeId(String nodeId, int limit) {
            return calls.stream().filter(call -> call.nodeId().equals(nodeId)).limit(limit).toList();
        }

        @Override
        public List<OcrNodeCall> listByNodeIdsAndDay(List<String> nodeIds, LocalDate day) {
            return calls.stream()
                    .filter(call -> nodeIds.contains(call.nodeId()) && call.startedAt().toLocalDate().equals(day))
                    .toList();
        }
    }

    /**
     * 固定调用记录 ID 生成器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static class FixedCallIdGenerator implements OcrCallIdGenerator {

        @Override
        public String newOcrCallId() {
            return "call-1";
        }
    }

    /**
     * 旧适配器降级哨兵。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static class LegacyFailingAdapter implements OcrAdapter {

        @Override
        public AdapterCapability capability() {
            return new AdapterCapability(ADAPTER_KEY, List.of("image"), true, false, true,
                    false, true, null, null, 1, 1, "legacy");
        }

        @Override
        public ImageOcrResult recognize(ImageOcrRequest request) {
            throw new AssertionError("legacy adapter should not be used when routing service is available");
        }
    }
}
