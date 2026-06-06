package com.doclens.ingestion.application;

import com.doclens.ingestion.domain.Batch;
import com.doclens.ingestion.domain.BatchRepository;
import com.doclens.processing.application.BatchProcessingUseCase;
import com.doclens.processing.domain.DocumentJob;
import com.doclens.processing.domain.DocumentJobCreateRequest;
import com.doclens.processing.domain.DocumentJobRepository;
import com.doclens.processing.domain.DocumentType;
import com.doclens.processing.domain.EventCreateRequest;
import com.doclens.processing.domain.OcrEvent;
import com.doclens.processing.domain.OcrEventFactory;
import com.doclens.processing.domain.OcrEventRepository;
import com.doclens.processing.domain.PdfMode;
import com.doclens.shared.application.TransactionRunner;
import com.doclens.shared.config.DocLensProperties;
import com.doclens.shared.domain.DocLensConstants;
import com.doclens.shared.domain.DuplicateResourceException;
import com.doclens.shared.domain.JsonPayload;
import com.doclens.shared.infrastructure.IdGenerator;
import com.doclens.storage.ObjectStorage;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Use case for creating OCR batches.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Service
public class CreateBatchUseCase {

    private final BatchRepository batchRepository;
    private final DocumentJobRepository documentRepository;
    private final OcrEventRepository eventRepository;
    private final ObjectStorage objectStorage;
    private final IdGenerator idGenerator;
    private final DocLensProperties properties;
    private final BatchProcessingUseCase batchProcessingUseCase;
    private final OcrEventFactory eventFactory;
    private final TransactionRunner transactionRunner;

    /**
     * Creates the use case.
     *
     * @param dependencies use case dependencies
     * @param transactionRunner transaction runner
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public CreateBatchUseCase(CreateBatchDependencies dependencies, TransactionRunner transactionRunner) {
        this.batchRepository = dependencies.batchRepository();
        this.documentRepository = dependencies.documentRepository();
        this.eventRepository = dependencies.eventRepository();
        this.objectStorage = dependencies.objectStorage();
        this.idGenerator = dependencies.idGenerator();
        this.properties = dependencies.properties();
        this.batchProcessingUseCase = dependencies.batchProcessingUseCase();
        this.eventFactory = dependencies.eventFactory();
        this.transactionRunner = transactionRunner;
    }

    /**
     * Creates a batch and its document jobs.
     *
     * @param command create batch command
     * @return upload response
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public Map<String, Object> create(CreateBatchCommand command) {
        validateCommand(command);
        Map<String, Object> response = transactionRunner.requiredResult(() -> createBatchRecords(command));
        triggerProcessing(String.valueOf(response.get("batch_id")));
        return response;
    }

    /**
     * Creates batch database records in a short transaction.
     *
     * @param command create batch command
     * @return upload response
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public Map<String, Object> createBatchRecords(CreateBatchCommand command) {
        checkIdempotency(command);
        OffsetDateTime now = OffsetDateTime.now();
        String batchId = idGenerator.newBatchId();
        JsonPayload metadata = new JsonPayload(command.metadata());
        Batch batch = Batch.create(batchId, command.files().size(), metadata, optionalText(command.callbackUrl()),
                optionalText(command.idempotencyKey()), now);
        batchRepository.save(batch);
        BatchDocumentPlan plan = new BatchDocumentPlan(command, batchId, metadata, now);
        List<DocumentJob> documents = createDocuments(plan);
        documentRepository.saveAll(documents);
        eventRepository.saveAll(initialEvents(plan, documents));
        return uploadResponse(batchId, documents);
    }

    private void triggerProcessing(String batchId) {
        if (properties.autoProcessOnUpload()) {
            batchProcessingUseCase.processBatch(batchId);
        } else {
            // Configuration intentionally leaves the batch queued for external workers.
        }
    }

    private void validateCommand(CreateBatchCommand command) {
        if (command.files() == null || command.files().isEmpty()) {
            throw new IllegalArgumentException("files is required");
        } else {
            command.files().forEach(file -> {
                if (file.content() == null || file.content().length == 0) {
                    throw new IllegalArgumentException("uploaded file is empty");
                } else {
                    // File payload is present.
                }
            });
        }
    }

    private void checkIdempotency(CreateBatchCommand command) {
        if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()
                && batchRepository.findByIdempotencyKey(command.idempotencyKey()).isPresent()) {
            throw new DuplicateResourceException("duplicate idempotency key");
        } else {
            // Empty idempotency key means normal non-idempotent creation.
        }
    }

    private List<DocumentJob> createDocuments(BatchDocumentPlan plan) {
        List<DocumentJob> documents = new ArrayList<>(plan.command().files().size());
        for (int index = 0; index < plan.command().files().size(); index++) {
            documents.add(createDocument(plan, plan.command().files().get(index), index));
        }
        return documents;
    }

    private DocumentJob createDocument(BatchDocumentPlan plan, UploadFileCommand file, int index) {
        String documentId = idGenerator.newDocumentId();
        DocumentType fileType = resolveFileType(file.fileName());
        String objectKey = "uploads/%s/%s/%s".formatted(plan.batchId(), documentId, sanitizeFileName(file.fileName()));
        String storageUri = objectStorage.writeBytes(objectKey, file.content());
        return DocumentJob.create(new DocumentJobCreateRequest(documentId, plan.batchId(), file.fileName(),
                fileType, file.content().length, DocLensConstants.DEFAULT_PAGE_COUNT, storageUri,
                resolveAdapter(plan.command()), resolvePdfMode(plan.command(), fileType), plan.metadata(), index, plan.now()));
    }

    private List<OcrEvent> initialEvents(BatchDocumentPlan plan, List<DocumentJob> documents) {
        List<OcrEvent> events = new ArrayList<>(documents.size() + 1);
        events.add(batchCreatedEvent(plan));
        documents.stream().map(document -> documentEnqueuedEvent(plan, document)).forEach(events::add);
        return events;
    }

    private OcrEvent batchCreatedEvent(BatchDocumentPlan plan) {
        return eventFactory.create(new EventCreateRequest(plan.batchId(), Optional.empty(),
                DocLensConstants.EVENT_BATCH_CREATED, DocLensConstants.STAGE_QUEUED, DocLensConstants.STAGE_QUEUED,
                Map.of("percent", DocLensConstants.ZERO_PROGRESS_PERCENT), plan.metadata(), Optional.empty(),
                Map.of(), Map.of()));
    }

    private OcrEvent documentEnqueuedEvent(BatchDocumentPlan plan, DocumentJob document) {
        return eventFactory.create(new EventCreateRequest(plan.batchId(), Optional.of(document.documentId()),
                DocLensConstants.EVENT_DOCUMENT_ENQUEUED, DocLensConstants.STAGE_QUEUED, DocLensConstants.STAGE_QUEUED,
                Map.of("percent", DocLensConstants.ZERO_PROGRESS_PERCENT), plan.metadata(), Optional.empty(),
                Map.of(), Map.of()));
    }

    private DocumentType resolveFileType(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".pdf")) {
            return DocumentType.PDF;
        } else if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg") || lowerName.endsWith(".webp")) {
            return DocumentType.IMAGE;
        } else {
            return DocumentType.WORD;
        }
    }

    private String resolveAdapter(CreateBatchCommand command) {
        if (command.adapterOverride() != null && !command.adapterOverride().isBlank()) {
            return command.adapterOverride();
        } else {
            return DocLensConstants.DEFAULT_ADAPTER_KEY;
        }
    }

    private Optional<PdfMode> resolvePdfMode(CreateBatchCommand command, DocumentType fileType) {
        if (fileType != DocumentType.PDF) {
            return Optional.empty();
        } else if (command.pdfMode() == null || command.pdfMode().isBlank()) {
            return Optional.of(PdfMode.PAGE_IMAGE_FALLBACK);
        } else {
            return Optional.of(PdfMode.valueOf(command.pdfMode().toUpperCase()));
        }
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private Map<String, Object> uploadResponse(String batchId, List<DocumentJob> documents) {
        List<Map<String, Object>> summaries = documents.stream().map(this::documentSummary).toList();
        return Map.of("batch_id", batchId, "status", DocLensConstants.STAGE_QUEUED,
                "total_files", summaries.size(), "documents", summaries);
    }

    private Map<String, Object> documentSummary(DocumentJob document) {
        return Map.of("document_id", document.documentId(), "file_name", document.fileName(),
                "status", DocLensConstants.STAGE_QUEUED);
    }

    private Optional<String> optionalText(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        } else {
            return Optional.of(value);
        }
    }
}
