package io.github.lvdaxianer.doclens.j.api;

import io.github.lvdaxianer.doclens.j.adapter.domain.AdapterRegistry;
import io.github.lvdaxianer.doclens.j.ingestion.application.CreateBatchCommand;
import io.github.lvdaxianer.doclens.j.ingestion.application.CreateBatchUseCase;
import io.github.lvdaxianer.doclens.j.ingestion.application.UploadFileCommand;
import io.github.lvdaxianer.doclens.j.query.application.OcrQueryService;
import java.util.List;
import java.util.Map;

/**
 * SDK 与 HTTP 模式共享的默认 DocLens 引擎实现。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class DefaultDocLensEngine implements DocLensEngine {

    private final CreateBatchUseCase createBatchUseCase;
    private final OcrQueryService queryService;
    private final AdapterRegistry adapterRegistry;

    /**
     * 创建 DocLens 引擎。
     *
     * @param createBatchUseCase 创建批次用例
     * @param queryService OCR 查询服务
     * @param adapterRegistry 适配器注册表
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public DefaultDocLensEngine(
            CreateBatchUseCase createBatchUseCase,
            OcrQueryService queryService,
            AdapterRegistry adapterRegistry
    ) {
        this.createBatchUseCase = createBatchUseCase;
        this.queryService = queryService;
        this.adapterRegistry = adapterRegistry;
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
    public Map<String, Object> getEvents(String batchId) {
        return queryService.getEvents(batchId);
    }

    @Override
    public Map<String, List<AdapterCapability>> listAdapters() {
        return Map.of("adapters", adapterRegistry.listCapabilities());
    }

    private CreateBatchCommand toCommand(CreateBatchRequest request) {
        return new CreateBatchCommand(toUploadFiles(request.files()), request.metadata(), request.callbackUrl(),
                request.idempotencyKey(), request.adapterOverride(), request.pdfMode());
    }

    private List<UploadFileCommand> toUploadFiles(List<DocumentInput> files) {
        return files.stream()
                .map(file -> new UploadFileCommand(file.fileName(), file.content()))
                .toList();
    }
}
