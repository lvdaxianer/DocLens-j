package com.doclens.processing.application;

import com.doclens.adapter.domain.OcrAdapter;
import com.doclens.adapter.infrastructure.DefaultAdapterRegistry;
import com.doclens.ingestion.domain.BatchRepository;
import com.doclens.ingestion.domain.BatchStatus;
import com.doclens.processing.domain.DocumentJob;
import com.doclens.processing.domain.DocumentJobRepository;
import com.doclens.processing.domain.DocumentStatus;
import com.doclens.processing.domain.EventCreateRequest;
import com.doclens.processing.domain.OcrEvent;
import com.doclens.processing.domain.OcrEventFactory;
import com.doclens.processing.domain.OcrEventRepository;
import com.doclens.processing.domain.OcrResult;
import com.doclens.processing.domain.OcrResultRepository;
import com.doclens.shared.application.TransactionRunner;
import com.doclens.shared.domain.DocLensConstants;
import com.doclens.shared.infrastructure.IdGenerator;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Use case for processing one batch in document order.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Service
public class BatchProcessingUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchProcessingUseCase.class);

    private final DocumentJobRepository documentRepository;
    private final OcrResultRepository resultRepository;
    private final OcrEventRepository eventRepository;
    private final BatchRepository batchRepository;
    private final DefaultAdapterRegistry adapterRegistry;
    private final IdGenerator idGenerator;
    private final OcrEventFactory eventFactory;
    private final TransactionRunner transactionRunner;

    /**
     * Creates batch processing use case.
     *
     * @param dependencies use case dependencies
     * @param transactionRunner transaction runner
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public BatchProcessingUseCase(BatchProcessingDependencies dependencies, TransactionRunner transactionRunner) {
        this.documentRepository = dependencies.documentRepository();
        this.resultRepository = dependencies.resultRepository();
        this.eventRepository = dependencies.eventRepository();
        this.batchRepository = dependencies.batchRepository();
        this.adapterRegistry = dependencies.adapterRegistry();
        this.idGenerator = dependencies.idGenerator();
        this.eventFactory = dependencies.eventFactory();
        this.transactionRunner = transactionRunner;
    }

    /**
     * Processes every document in a batch.
     *
     * @param batchId batch id
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public void processBatch(String batchId) {
        List<DocumentJob> documents = documentRepository.listByBatchId(batchId);
        List<DocumentProcessingResult> results = documents.stream().map(this::processDocument).toList();
        transactionRunner.requiredVoid(() -> persistBatchProcessing(batchId, results));
    }

    private DocumentProcessingResult processDocument(DocumentJob document) {
        try {
            DocumentJob started = startDocument(document);
            DocumentJob progressed = started.markPageCompleted(1, Math.max(1, started.totalPages()), OffsetDateTime.now());
            return completeDocument(started, progressed);
        } catch (RuntimeException ex) {
            LOGGER.warn("[OCR处理] 文档处理失败 documentId={}, error={}", document.documentId(), ex.getMessage(), ex);
            return failDocument(document, ex);
        }
    }

    private DocumentJob startDocument(DocumentJob document) {
        return document.startProcessing(OffsetDateTime.now());
    }

    private DocumentProcessingResult completeDocument(DocumentJob started, DocumentJob progressed) {
        OcrResult result = buildResult(progressed);
        DocumentJob completed = progressed.complete(result.resultId(), OffsetDateTime.now());
        return new DocumentProcessingResult(completed, Optional.of(result), completionEvents(started, progressed, completed));
    }

    private OcrResult buildResult(DocumentJob document) {
        OcrAdapter adapter = adapterRegistry.find(document.adapterName())
                .orElseThrow(() -> new IllegalArgumentException("adapter not found: " + document.adapterName()));
        Map<String, Object> rawOutput = adapter.parse(document);
        String resultId = idGenerator.newResultId();
        List<Map<String, Object>> pageText = pageText(document);
        return new OcrResult(resultId, document.documentId(), rawOutput, structuredDocument(document, rawOutput),
                pageText, layoutBlocks(pageText), List.of(), List.of(), DocLensConstants.STUB_CONFIDENCE, List.of(),
                OffsetDateTime.now());
    }

    private Map<String, Object> structuredDocument(DocumentJob document, Map<String, Object> rawOutput) {
        return Map.of(
                "documentId", document.documentId(),
                "fileName", document.fileName(),
                "pages", rawOutput.get("pages")
        );
    }

    private List<Map<String, Object>> pageText(DocumentJob document) {
        return List.of(Map.of("pageNo", DocLensConstants.DEFAULT_PAGE_NO, "text", "Stub OCR text for " + document.fileName()));
    }

    private List<Map<String, Object>> layoutBlocks(List<Map<String, Object>> pageText) {
        return List.of(Map.of("pageNo", DocLensConstants.DEFAULT_PAGE_NO, "type", "paragraph",
                "text", pageText.getFirst().get("text")));
    }

    private DocumentProcessingResult failDocument(DocumentJob document, RuntimeException ex) {
        DocumentJob failed = document.fail(DocLensConstants.ERROR_CODE_OCR_FAILED, ex.getMessage(), OffsetDateTime.now());
        OcrEvent event = event(new DocumentEventPlan(failed, DocLensConstants.EVENT_DOCUMENT_FAILED,
                Map.of("percent", DocLensConstants.COMPLETED_PROGRESS_PERCENT),
                Map.of("code", DocLensConstants.ERROR_CODE_OCR_FAILED, "message", ex.getMessage())));
        return new DocumentProcessingResult(failed, Optional.empty(), List.of(event));
    }

    private List<OcrEvent> completionEvents(DocumentJob started, DocumentJob progressed, DocumentJob completed) {
        return List.of(
                event(new DocumentEventPlan(started, DocLensConstants.EVENT_DOCUMENT_STARTED,
                        Map.of("percent", DocLensConstants.START_PROGRESS_PERCENT), Map.of())),
                event(new DocumentEventPlan(progressed, DocLensConstants.EVENT_DOCUMENT_PAGE_COMPLETED,
                        pageProgress(progressed), Map.of())),
                event(new DocumentEventPlan(completed, DocLensConstants.EVENT_DOCUMENT_COMPLETED,
                        Map.of("percent", DocLensConstants.COMPLETED_PROGRESS_PERCENT), resultSummary()))
        );
    }

    private Map<String, Object> pageProgress(DocumentJob document) {
        return Map.of("percent", document.progressPercent(), "current_page", DocLensConstants.DEFAULT_PAGE_NO,
                "total_pages", document.totalPages());
    }

    private Map<String, Object> resultSummary() {
        return Map.of("pageCount", DocLensConstants.DEFAULT_PAGE_COUNT, "blockCount", DocLensConstants.DEFAULT_BLOCK_COUNT,
                "tableCount", DocLensConstants.DEFAULT_TABLE_COUNT, "confidence", DocLensConstants.STUB_CONFIDENCE);
    }

    public void persistBatchProcessing(String batchId, List<DocumentProcessingResult> results) {
        documentRepository.updateAll(results.stream().map(DocumentProcessingResult::document).toList());
        resultRepository.saveAll(results.stream().flatMap(result -> result.result().stream()).toList());
        eventRepository.saveAll(flattenEvents(results));
        finishBatch(batchId);
    }

    private List<OcrEvent> flattenEvents(List<DocumentProcessingResult> results) {
        List<OcrEvent> events = new ArrayList<>(results.size() * 3);
        results.stream().flatMap(result -> result.events().stream()).forEach(events::add);
        return events;
    }

    private void finishBatch(String batchId) {
        List<DocumentJob> documents = documentRepository.listByBatchId(batchId);
        long completed = documents.stream().filter(document -> document.status() == DocumentStatus.COMPLETED).count();
        long failed = documents.stream().filter(document -> document.status() == DocumentStatus.FAILED).count();
        BatchStatus status = resolveBatchStatus(documents.size(), completed, failed);
        batchRepository.updateSummary(batchId, Math.toIntExact(completed), Math.toIntExact(failed), status);
        if (!documents.isEmpty()) {
            DocumentJob first = documents.getFirst();
            eventRepository.save(batchFinishedEvent(batchId, status, first));
        } else {
            // Empty batch cannot be created by the ingestion use case.
        }
    }

    private OcrEvent batchFinishedEvent(String batchId, BatchStatus status, DocumentJob first) {
        return eventFactory.create(new EventCreateRequest(batchId, Optional.empty(),
                "batch." + status.name().toLowerCase(), status.name().toLowerCase(), status.name().toLowerCase(),
                Map.of("percent", DocLensConstants.COMPLETED_PROGRESS_PERCENT), first.metadata(), Optional.empty(),
                Map.of(), Map.of()));
    }

    private BatchStatus resolveBatchStatus(int total, long completed, long failed) {
        if (failed == 0 && completed == total) {
            return BatchStatus.COMPLETED;
        } else if (completed == 0 && failed == total) {
            return BatchStatus.FAILED;
        } else {
            return BatchStatus.PARTIAL_FAILED;
        }
    }

    private OcrEvent event(DocumentEventPlan plan) {
        DocumentJob document = plan.document();
        return eventFactory.create(new EventCreateRequest(document.batchId(), Optional.of(document.documentId()),
                plan.eventType(), document.status().name().toLowerCase(), document.stage().name().toLowerCase(),
                plan.progress(), document.metadata(), document.resultId(), summaryDetail(plan), errorDetail(plan)));
    }

    private Map<String, Object> summaryDetail(DocumentEventPlan plan) {
        if (DocLensConstants.EVENT_DOCUMENT_COMPLETED.equals(plan.eventType())) {
            return plan.detail();
        } else {
            return Map.of();
        }
    }

    private Map<String, Object> errorDetail(DocumentEventPlan plan) {
        if (DocLensConstants.EVENT_DOCUMENT_COMPLETED.equals(plan.eventType())) {
            return Map.of();
        } else {
            return plan.detail();
        }
    }
}
