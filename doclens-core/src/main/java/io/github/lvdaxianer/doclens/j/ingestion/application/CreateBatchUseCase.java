package io.github.lvdaxianer.doclens.j.ingestion.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.domain.EventCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.PdfMode;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 创建 OCR 批次的用例。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class CreateBatchUseCase {

    private final BatchRepository batchRepository;
    private final DocumentJobRepository documentRepository;
    private final OcrEventRepository eventRepository;
    private final ObjectStorage objectStorage;
    private final IdGenerator idGenerator;
    private final DocLensProperties properties;
    private final BatchProcessingScheduler batchProcessingScheduler;
    private final OcrEventFactory eventFactory;
    private final TransactionRunner transactionRunner;

    /**
     * 创建用例。
     *
     * @param dependencies 用例依赖
     * @param transactionRunner 事务执行器
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
        this.batchProcessingScheduler = dependencies.batchProcessingScheduler();
        this.eventFactory = dependencies.eventFactory();
        this.transactionRunner = transactionRunner;
    }

    /**
     * 创建批次及其文档任务。
     *
     * @param command 创建批次命令
     * @return 上传响应
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
     * 在短事务中创建批次数据库记录。
     *
     * @param command 创建批次命令
     * @return 上传响应
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public Map<String, Object> createBatchRecords(CreateBatchCommand command) {
        OffsetDateTime now = OffsetDateTime.now();
        String batchId = idGenerator.newBatchId();
        JsonPayload metadata = new JsonPayload(command.metadata());
        Batch batch = Batch.create(batchId, command.files().size(), metadata, optionalText(command.callbackUrl()),
                optionalText(command.idempotencyKey()), now, command.callerIdentity());
        batchRepository.save(batch);
        BatchDocumentPlan plan = new BatchDocumentPlan(command, batchId, metadata, now);
        List<DocumentJob> documents = createDocuments(plan);
        documentRepository.saveAll(documents);
        eventRepository.saveAll(initialEvents(plan, documents));
        return uploadResponse(batch, documents);
    }

    private void triggerProcessing(String batchId) {
        if (properties.autoProcessOnUpload()) {
            batchProcessingScheduler.schedule(batchId);
        } else {
            // 配置明确要求批次保持排队状态，等待外部 Worker 处理。
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
                    // 文件载荷已存在。
                }
            });
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
                resolveAdapter(plan.command()), resolvePdfMode(plan.command(), fileType),
                plan.command().ocrRoutePolicy(), plan.metadata(), index, plan.now(), plan.command().chunkStrategy(),
                plan.command().llmOrchestrated()));
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
                || lowerName.endsWith(".jpeg") || lowerName.endsWith(".webp")
                || lowerName.endsWith(".tif") || lowerName.endsWith(".tiff")) {
            return DocumentType.IMAGE;
        } else if (lowerName.endsWith(".md") || lowerName.endsWith(".markdown")) {
            return DocumentType.MARKDOWN;
        } else if (lowerName.endsWith(".txt")) {
            return DocumentType.TEXT;
        } else {
            return DocumentType.WORD;
        }
    }

    private String resolveAdapter(CreateBatchCommand command) {
        if (command.adapterOverride() != null && !command.adapterOverride().isBlank()) {
            return command.adapterOverride();
        } else if (properties.adapter() != null && properties.adapter().defaultKey() != null
                && !properties.adapter().defaultKey().isBlank()) {
            return properties.adapter().defaultKey();
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

    private Map<String, Object> uploadResponse(Batch batch, List<DocumentJob> documents) {
        List<Map<String, Object>> summaries = documents.stream().map(this::documentSummary).toList();
        CallerIdentity callerIdentity = batch.callerIdentity();
        return Map.ofEntries(
                Map.entry("batch_id", batch.batchId()),
                Map.entry("status", DocLensConstants.STAGE_QUEUED),
                Map.entry("total_files", summaries.size()),
                Map.entry("documents", summaries),
                Map.entry(CallerIdentity.CLIENT_ID_FIELD, callerIdentity.clientId()),
                Map.entry(CallerIdentity.SOURCE_APP_FIELD, callerIdentity.sourceApp()),
                Map.entry(CallerIdentity.TENANT_KEY_FIELD, callerIdentity.tenantKey().orElse(""))
        );
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
