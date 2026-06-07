package io.github.lvdaxianer.doclens.j.query.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.shared.domain.ResourceNotFoundException;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import java.util.List;
import java.util.Map;

/**
 * Query service for OCR public read models.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class OcrQueryService {

    private final BatchRepository batchRepository;
    private final DocumentJobRepository documentRepository;
    private final OcrResultRepository resultRepository;
    private final OcrEventRepository eventRepository;

    /**
     * Creates OCR query service.
     *
     * @param batchRepository batch repository
     * @param documentRepository document repository
     * @param resultRepository result repository
     * @param eventRepository event repository
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public OcrQueryService(
            BatchRepository batchRepository,
            DocumentJobRepository documentRepository,
            OcrResultRepository resultRepository,
            OcrEventRepository eventRepository
    ) {
        this.batchRepository = batchRepository;
        this.documentRepository = documentRepository;
        this.resultRepository = resultRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * Gets a batch read model.
     *
     * @param batchId batch id
     * @return batch read model
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public Map<String, Object> getBatch(String batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("batch " + batchId + " not found"));
        return Map.ofEntries(
                Map.entry("batch_id", batch.batchId()),
                Map.entry("status", batch.status().name().toLowerCase()),
                Map.entry("total_files", batch.totalFiles()),
                Map.entry("completed_files", batch.completedFiles()),
                Map.entry("failed_files", batch.failedFiles()),
                Map.entry("progress_percent", batchProgress(batch)),
                Map.entry("metadata", batch.metadata().values()),
                Map.entry("created_at", batch.createdAt().toString()),
                Map.entry("updated_at", batch.updatedAt().toString())
        );
    }

    /**
     * Gets a document read model.
     *
     * @param documentId document id
     * @return document read model
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public Map<String, Object> getDocument(String documentId) {
        DocumentJob document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("document " + documentId + " not found"));
        return Map.ofEntries(
                Map.entry("document_id", document.documentId()),
                Map.entry("batch_id", document.batchId()),
                Map.entry("file_name", document.fileName()),
                Map.entry("file_type", document.fileType().name().toLowerCase()),
                Map.entry("status", document.status().name().toLowerCase()),
                Map.entry("stage", document.stage().name().toLowerCase()),
                Map.entry("progress_percent", document.progressPercent()),
                Map.entry("current_page", document.currentPage()),
                Map.entry("total_pages", document.totalPages()),
                Map.entry("adapter_name", document.adapterName()),
                Map.entry("metadata", document.metadata().values()),
                Map.entry("result_id", document.resultId().orElse(DocLensConstants.EMPTY_VALUE)),
                Map.entry("error", documentError(document)),
                Map.entry("created_at", document.createdAt().toString()),
                Map.entry("updated_at", document.updatedAt().toString())
        );
    }

    /**
     * Gets OCR result read model.
     *
     * @param documentId document id
     * @return result read model
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public Map<String, Object> getDocumentResult(String documentId) {
        OcrResult result = resultRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("document result " + documentId + " not found"));
        Map<String, Object> summary = Map.of(
                "pageCount", result.pageText().size(),
                "blockCount", result.layoutBlocks().size(),
                "tableCount", result.tables().size(),
                "confidence", result.confidence()
        );
        Map<String, Object> payload = Map.ofEntries(
                Map.entry("pages", result.structuredDocument().getOrDefault("pages", List.of())),
                Map.entry("structuredDocument", result.structuredDocument()),
                Map.entry("rawVendorOutput", result.rawVendorOutput()),
                Map.entry("pageText", result.pageText()),
                Map.entry("layoutBlocks", result.layoutBlocks()),
                Map.entry("tables", result.tables()),
                Map.entry("images", result.images()),
                Map.entry("confidence", result.confidence()),
                Map.entry("warnings", result.warnings()),
                Map.entry("summary", summary)
        );
        return Map.of("document_id", documentId, "result_id", result.resultId(), "result", payload);
    }

    /**
     * Gets event timeline for a batch.
     *
     * @param batchId batch id
     * @return event timeline
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public Map<String, Object> getEvents(String batchId) {
        List<Map<String, Object>> events = eventRepository.listByBatchId(batchId).stream().map(this::eventView).toList();
        return Map.of("batch_id", batchId, "events", events);
    }

    private int batchProgress(Batch batch) {
        if (batch.totalFiles() > 0) {
            return (int) Math.round((batch.completedFiles() + batch.failedFiles()) * 100.0 / batch.totalFiles());
        } else {
            return 0;
        }
    }

    private Map<String, Object> documentError(DocumentJob document) {
        if (document.errorCode().isEmpty()) {
            return Map.of();
        } else {
            return Map.of("code", document.errorCode().orElse(DocLensConstants.EMPTY_VALUE),
                    "message", document.errorMessage().orElse(DocLensConstants.EMPTY_VALUE));
        }
    }

    private Map<String, Object> eventView(OcrEvent event) {
        return Map.ofEntries(
                Map.entry("event_id", event.eventId()),
                Map.entry("event_type", event.eventType()),
                Map.entry("batch_id", event.batchId()),
                Map.entry("document_id", event.documentId().orElse(DocLensConstants.EMPTY_VALUE)),
                Map.entry("status", event.status()),
                Map.entry("stage", event.stage()),
                Map.entry("progress", event.progress()),
                Map.entry("metadata", event.metadata().values()),
                Map.entry("result_id", event.resultId().orElse(DocLensConstants.EMPTY_VALUE)),
                Map.entry("result_summary", event.resultSummary()),
                Map.entry("error", event.error()),
                Map.entry("occurred_at", event.occurredAt().toString())
        );
    }
}
