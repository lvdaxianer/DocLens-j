package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.DefaultAdapterRegistry;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.EventCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentProgressReporter;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionRequest;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionResult;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractor;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 按文档顺序处理单个批次的用例。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class BatchProcessingUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchProcessingUseCase.class);

    private final DocumentJobRepository documentRepository;
    private final OcrResultRepository resultRepository;
    private final OcrEventRepository eventRepository;
    private final BatchRepository batchRepository;
    private final DefaultAdapterRegistry adapterRegistry;
    private final ObjectStorage objectStorage;
    private final DocumentTextExtractor documentTextExtractor;
    private final IdGenerator idGenerator;
    private final OcrEventFactory eventFactory;
    private final TransactionRunner transactionRunner;

    /**
     * 创建批次处理用例。
     *
     * @param dependencies 用例依赖
     * @param transactionRunner 事务执行器
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public BatchProcessingUseCase(BatchProcessingDependencies dependencies, TransactionRunner transactionRunner) {
        this.documentRepository = dependencies.documentRepository();
        this.resultRepository = dependencies.resultRepository();
        this.eventRepository = dependencies.eventRepository();
        this.batchRepository = dependencies.batchRepository();
        this.adapterRegistry = dependencies.adapterRegistry();
        this.objectStorage = dependencies.objectStorage();
        this.documentTextExtractor = dependencies.documentTextExtractor();
        this.idGenerator = dependencies.idGenerator();
        this.eventFactory = dependencies.eventFactory();
        this.transactionRunner = transactionRunner;
    }

    /**
     * 处理批次中的每个文档。
     *
     * @param batchId 批次 ID
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public void processBatch(String batchId) {
        List<DocumentJob> documents = documentRepository.listByBatchId(batchId);
        documents.stream()
                .map(this::processDocument)
                .forEach(result -> transactionRunner.requiredVoid(() -> persistDocumentProcessing(result)));
        transactionRunner.requiredVoid(() -> finishBatch(batchId));
    }

    private DocumentProcessingResult processDocument(DocumentJob document) {
        try {
            DocumentJob started = startDocument(document);
            return completeDocument(started);
        } catch (RuntimeException ex) {
            LOGGER.warn("[OCR处理] 文档处理失败 documentId={}, error={}", document.documentId(), ex.getMessage(), ex);
            return failDocument(document, ex);
        }
    }

    private DocumentJob startDocument(DocumentJob document) {
        return document.startProcessing(OffsetDateTime.now());
    }

    private DocumentProcessingResult completeDocument(DocumentJob started) {
        OcrResult result = buildResult(started);
        int pageCount = Math.max(DocLensConstants.DEFAULT_PAGE_COUNT, result.pageText().size());
        DocumentJob progressed = started.markPageCompleted(pageCount, pageCount, OffsetDateTime.now());
        DocumentJob saving = progressed.advanceStage(ProcessingStage.SAVE_TEXT, pageCount, pageCount,
                OffsetDateTime.now());
        DocumentJob completed = saving.complete(result.resultId(), OffsetDateTime.now());
        return new DocumentProcessingResult(completed, Optional.of(result),
                completionEvents(started, progressed, saving, completed, result));
    }

    private OcrResult buildResult(DocumentJob document) {
        adapterRegistry.find(document.adapterName())
                .orElseThrow(() -> new IllegalArgumentException("adapter not found: " + document.adapterName()));
        byte[] content = objectStorage.readBytes(document.storageUri());
        DocumentTextExtractionResult extracted = documentTextExtractor.extract(
                new DocumentTextExtractionRequest(document, content, document.adapterName(),
                        stageReporter(document)));
        stageReporter(document).report(ProcessingStage.MERGE_TEXT, extracted.pageText().size(),
                extracted.pageText().size());
        stageReporter(document).report(ProcessingStage.SAVE_TEXT, extracted.pageText().size(),
                extracted.pageText().size());
        String markdownStorageUri = writeMarkdownResult(document, extracted.finalText());
        String resultId = idGenerator.newResultId();
        return new OcrResult(resultId, document.documentId(), extracted.finalText(), markdownStorageUri,
                extracted.rawOutput(), extracted.structuredDocument(), extracted.pageText(), extracted.layoutBlocks(),
                extracted.tables(), extracted.images(), extracted.confidence(), extracted.warnings(), OffsetDateTime.now());
    }

    private String writeMarkdownResult(DocumentJob document, String finalText) {
        String objectKey = "results/%s/%s/%s".formatted(document.batchId(), document.documentId(),
                MarkdownResultNamer.markdownFileName(document.fileName(), UUID.randomUUID()));
        return objectStorage.writeBytes(objectKey, finalText.getBytes(StandardCharsets.UTF_8));
    }

    private DocumentProcessingResult failDocument(DocumentJob document, RuntimeException ex) {
        DocumentJob failed = latestDocument(document).fail(DocLensConstants.ERROR_CODE_OCR_FAILED, ex.getMessage(),
                OffsetDateTime.now());
        OcrEvent event = event(new DocumentEventPlan(failed, DocLensConstants.EVENT_DOCUMENT_FAILED,
                Map.of("percent", DocLensConstants.COMPLETED_PROGRESS_PERCENT),
                Map.of("code", DocLensConstants.ERROR_CODE_OCR_FAILED, "message", ex.getMessage())));
        return new DocumentProcessingResult(failed, Optional.empty(), List.of(event));
    }

    private DocumentJob latestDocument(DocumentJob document) {
        return documentRepository.findById(document.documentId()).orElse(document);
    }

    private List<OcrEvent> completionEvents(
            DocumentJob started,
            DocumentJob progressed,
            DocumentJob saving,
            DocumentJob completed,
            OcrResult result
    ) {
        return List.of(
                event(new DocumentEventPlan(started, DocLensConstants.EVENT_DOCUMENT_STARTED,
                        Map.of("percent", DocLensConstants.START_PROGRESS_PERCENT), Map.of())),
                event(new DocumentEventPlan(progressed, DocLensConstants.EVENT_DOCUMENT_PAGE_COMPLETED,
                        pageProgress(progressed), Map.of())),
                event(new DocumentEventPlan(saving, DocLensConstants.EVENT_DOCUMENT_STAGE_CHANGED,
                        pageProgress(saving), Map.of("stage", saving.stage().name().toLowerCase()))),
                event(new DocumentEventPlan(completed, DocLensConstants.EVENT_DOCUMENT_COMPLETED,
                        Map.of("percent", DocLensConstants.COMPLETED_PROGRESS_PERCENT), resultSummary(result)))
        );
    }

    private DocumentProgressReporter stageReporter(DocumentJob document) {
        return (stage, completedImages, totalImages) -> transactionRunner.requiredVoid(() ->
                documentRepository.update(progressDocument(document.documentId(), stage, completedImages, totalImages)));
    }

    private DocumentJob progressDocument(
            String documentId,
            ProcessingStage stage,
            int completedImages,
            int totalImages
    ) {
        DocumentJob current = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalStateException("document not found: " + documentId));
        return current.advanceStage(stage, completedImages, totalImages, OffsetDateTime.now());
    }

    private Map<String, Object> pageProgress(DocumentJob document) {
        return Map.of("percent", document.progressPercent(), "current_page", document.currentPage(),
                "total_pages", document.totalPages());
    }

    private Map<String, Object> resultSummary(OcrResult result) {
        return Map.of("pageCount", result.pageText().size(), "blockCount", result.layoutBlocks().size(),
                "tableCount", result.tables().size(), "confidence", result.confidence());
    }

    void persistDocumentProcessing(DocumentProcessingResult result) {
        documentRepository.updateAll(List.of(result.document()));
        resultRepository.saveAll(result.result().stream().toList());
        eventRepository.saveAll(result.events());
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
            // 接收入库用例不会创建空批次。
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
        if (!DocLensConstants.EVENT_DOCUMENT_FAILED.equals(plan.eventType())) {
            return plan.detail();
        } else {
            return Map.of();
        }
    }

    private Map<String, Object> errorDetail(DocumentEventPlan plan) {
        if (DocLensConstants.EVENT_DOCUMENT_FAILED.equals(plan.eventType())) {
            return plan.detail();
        } else {
            return Map.of();
        }
    }
}
