package io.github.lvdaxianer.doclens.j.api;

import io.github.lvdaxianer.doclens.j.adapter.domain.AdapterRegistry;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutingMode;
import io.github.lvdaxianer.doclens.j.ingestion.application.CreateBatchCommand;
import io.github.lvdaxianer.doclens.j.ingestion.application.CreateBatchUseCase;
import io.github.lvdaxianer.doclens.j.ingestion.application.UploadFileCommand;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCase;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentRetryUseCase;
import io.github.lvdaxianer.doclens.j.query.application.OcrQueryService;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * SDK 与 HTTP 模式共享的默认 DocLens 引擎实现。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class DefaultDocLensEngine implements DocLensEngine {

    private static final Map<OcrRoutingMode, Function<CreateBatchRequest, OcrRoutePolicy>> ROUTE_POLICY_FACTORIES =
            routePolicyFactories();

    private final CreateBatchUseCase createBatchUseCase;
    private final OcrQueryService queryService;
    private final AdapterRegistry adapterRegistry;
    private final DocumentRetryUseCase documentRetryUseCase;
    private final DocumentDeleteUseCase documentDeleteUseCase;

    /**
     * 创建 DocLens 引擎。
     *
     * @param createBatchUseCase 创建批次用例
     * @param queryService OCR 查询服务
     * @param adapterRegistry 适配器注册表
     * @param documentRetryUseCase 文档重试用例
     * @param documentDeleteUseCase 文档删除用例
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public DefaultDocLensEngine(
            CreateBatchUseCase createBatchUseCase,
            OcrQueryService queryService,
            AdapterRegistry adapterRegistry,
            DocumentRetryUseCase documentRetryUseCase,
            DocumentDeleteUseCase documentDeleteUseCase
    ) {
        this.createBatchUseCase = createBatchUseCase;
        this.queryService = queryService;
        this.adapterRegistry = adapterRegistry;
        this.documentRetryUseCase = documentRetryUseCase;
        this.documentDeleteUseCase = documentDeleteUseCase;
    }

    @Override
    public Map<String, Object> createBatch(CreateBatchRequest request) {
        return createBatchUseCase.create(toCommand(request));
    }

    @Override
    public Map<String, Object> getBatch(String batchId) {
        return queryService.getBatch(batchId);
    }

    @Override
    public Map<String, Object> getDocument(String documentId) {
        return queryService.getDocument(documentId);
    }

    @Override
    public Map<String, Object> getDocumentResult(String documentId) {
        return queryService.getDocumentResult(documentId);
    }

    @Override
    public Map<String, Object> retryDocument(String documentId) {
        documentRetryUseCase.retry(documentId);
        return Map.of("document_id", documentId, "status", "queued");
    }

    @Override
    public Map<String, Object> deleteDocument(String documentId) {
        documentDeleteUseCase.delete(documentId);
        return Map.of("document_id", documentId, "status", "deleted");
    }

    @Override
    public Map<String, Object> getEvents(String batchId) {
        return queryService.getEvents(batchId);
    }

    @Override
    public Map<String, List<AdapterCapability>> listAdapters() {
        return Map.of("adapters", adapterRegistry.listCapabilities());
    }

    private CreateBatchCommand toCommand(CreateBatchRequest request) {
        return new CreateBatchCommand(toUploadFiles(request.files()), request.metadata(), request.callbackUrl(),
                request.idempotencyKey(), request.adapterOverride(), request.pdfMode(), toOcrRoutePolicy(request));
    }

    /**
     * 将 API 请求中的 OCR 路由字段转换为领域策略。
     *
     * @param request 创建批次请求
     * @return OCR 路由策略
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrRoutePolicy toOcrRoutePolicy(CreateBatchRequest request) {
        OcrRoutingMode routingMode = parseRoutingMode(request.ocrRoutingMode());
        return ROUTE_POLICY_FACTORIES.get(routingMode).apply(request);
    }

    /**
     * 解析缺省安全的 OCR 路由模式。
     *
     * @param routingMode 请求中的路由模式文本
     * @return OCR 路由模式
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrRoutingMode parseRoutingMode(String routingMode) {
        if (routingMode == null || routingMode.isBlank()) {
            return OcrRoutingMode.DEFAULT;
        } else {
            return OcrRoutingMode.valueOf(routingMode.trim().toUpperCase());
        }
    }

    /**
     * 创建 OCR 路由模式到策略工厂的映射。
     *
     * @return OCR 路由策略工厂映射
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static Map<OcrRoutingMode, Function<CreateBatchRequest, OcrRoutePolicy>> routePolicyFactories() {
        Map<OcrRoutingMode, Function<CreateBatchRequest, OcrRoutePolicy>> factories =
                new EnumMap<>(OcrRoutingMode.class);
        factories.put(OcrRoutingMode.DEFAULT, request -> OcrRoutePolicy.defaultPolicy());
        factories.put(OcrRoutingMode.GLOBAL_LOAD_BALANCE,
                request -> OcrRoutePolicy.globalLoadBalance(request.ocrLoadBalanceStrategy()));
        factories.put(OcrRoutingMode.MODEL_LOAD_BALANCE,
                request -> OcrRoutePolicy.modelLoadBalance(request.ocrModelKey(), request.ocrLoadBalanceStrategy()));
        factories.put(OcrRoutingMode.SPECIFIC_NODE,
                request -> OcrRoutePolicy.specificNode(request.ocrModelKey(), request.ocrNodeId()));
        return Map.copyOf(factories);
    }

    private List<UploadFileCommand> toUploadFiles(List<DocumentInput> files) {
        return files.stream()
                .map(file -> new UploadFileCommand(file.fileName(), file.content()))
                .toList();
    }
}
