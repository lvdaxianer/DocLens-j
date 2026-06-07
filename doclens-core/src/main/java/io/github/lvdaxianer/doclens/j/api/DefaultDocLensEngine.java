package io.github.lvdaxianer.doclens.j.api;

import io.github.lvdaxianer.doclens.j.ingestion.application.CreateBatchCommand;
import io.github.lvdaxianer.doclens.j.ingestion.application.CreateBatchUseCase;
import io.github.lvdaxianer.doclens.j.ingestion.application.UploadFileCommand;
import io.github.lvdaxianer.doclens.j.query.application.OcrQueryService;
import java.util.List;
import java.util.Map;

/**
 * Default DocLens engine implementation shared by SDK and HTTP modes.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class DefaultDocLensEngine implements DocLensEngine {

    private final CreateBatchUseCase createBatchUseCase;
    private final OcrQueryService queryService;

    /**
     * Creates DocLens engine.
     *
     * @param createBatchUseCase create batch use case
     * @param queryService OCR query service
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public DefaultDocLensEngine(CreateBatchUseCase createBatchUseCase, OcrQueryService queryService) {
        this.createBatchUseCase = createBatchUseCase;
        this.queryService = queryService;
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
