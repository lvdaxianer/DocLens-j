package io.github.lvdaxianer.doclens.j.query.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.shared.domain.ResourceNotFoundException;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * OCR 公开读模型查询服务。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class OcrQueryService {

    private static final String BATCH_ID_FIELD = "batch_id";
    private static final String IDEMPOTENCY_KEY_FIELD = "idempotency_key";
    private static final String STATUS_FIELD = "status";
    private static final String TOTAL_FILES_FIELD = "total_files";
    private static final String COMPLETED_FILES_FIELD = "completed_files";
    private static final String FAILED_FILES_FIELD = "failed_files";
    private static final String PROGRESS_PERCENT_FIELD = "progress_percent";
    private static final String DOCUMENTS_FIELD = "documents";
    private static final String DOCUMENT_ID_FIELD = "document_id";
    private static final String FILE_NAME_FIELD = "file_name";
    private static final String CURRENT_PAGE_FIELD = "current_page";
    private static final String TOTAL_PAGES_FIELD = "total_pages";
    private static final String RESULT_ID_FIELD = "result_id";
    private static final String ERROR_FIELD = "error";
    private static final String CODE_FIELD = "code";
    private static final String MESSAGE_FIELD = "message";
    private static final String CREATED_AT_FIELD = "created_at";
    private static final String UPDATED_AT_FIELD = "updated_at";
    private static final String EMPTY_VALUE = "";
    private static final String BATCH_NOT_FOUND_MESSAGE_TEMPLATE = "batch not found for idempotency key %s";

    private final BatchRepository batchRepository;
    private final DocumentJobRepository documentRepository;
    private final OcrResultRepository resultRepository;
    private final OcrEventRepository eventRepository;

    /**
     * 创建 OCR 查询服务。
     *
     * @param batchRepository 批次仓储
     * @param documentRepository 文档仓储
     * @param resultRepository 结果仓储
     * @param eventRepository 事件仓储
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
     * 获取批次读模型。
     *
     * @param batchId 批次 ID
     * @return 批次读模型
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public Map<String, Object> getBatch(String batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("batch " + batchId + " not found"));
        return batchView(batch);
    }

    /**
     * 获取 caller 范围内的批次读模型。
     *
     * @param caller caller 身份
     * @param batchId 批次 ID
     * @return 批次读模型
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public Map<String, Object> getBatch(CallerIdentity caller, String batchId) {
        return batchView(findCallerBatch(caller, batchId));
    }

    /**
     * 通过幂等键获取批次 reconciliation 视图。
     *
     * @param idempotencyKey 幂等键
     * @return reconciliation 批次视图
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    public Map<String, Object> getBatchByIdempotencyKey(String idempotencyKey) {
        Batch batch = batchRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BATCH_NOT_FOUND_MESSAGE_TEMPLATE, idempotencyKey)));
        List<Map<String, Object>> documents = documentRepository.listByBatchId(batch.batchId()).stream()
                .sorted(Comparator.comparingInt(DocumentJob::sortOrder))
                .map(this::reconciliationDocumentView)
                .toList();
        return reconciliationBatchView(batch, documents);
    }

    /**
     * 通过 caller 范围内幂等键获取批次 reconciliation 视图。
     *
     * @param caller caller 身份
     * @param idempotencyKey 幂等键
     * @return reconciliation 批次视图
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public Map<String, Object> getBatchByIdempotencyKey(CallerIdentity caller, String idempotencyKey) {
        Batch batch = batchRepository.findByIdempotencyKeyForCaller(caller, idempotencyKey)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BATCH_NOT_FOUND_MESSAGE_TEMPLATE, idempotencyKey)));
        List<Map<String, Object>> documents = documentRepository.listByBatchId(batch.batchId()).stream()
                .sorted(Comparator.comparingInt(DocumentJob::sortOrder))
                .map(this::reconciliationDocumentView)
                .toList();
        return reconciliationBatchView(batch, documents);
    }

    /**
     * 获取文档读模型。
     *
     * @param documentId 文档 ID
     * @return 文档读模型
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public Map<String, Object> getDocument(String documentId) {
        DocumentJob document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("document " + documentId + " not found"));
        return documentView(document);
    }

    /**
     * 获取 caller 范围内文档读模型。
     *
     * @param caller caller 身份
     * @param documentId 文档 ID
     * @return 文档读模型
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public Map<String, Object> getDocument(CallerIdentity caller, String documentId) {
        return documentView(findCallerDocument(caller, documentId));
    }

    /**
     * 构建文档读模型。
     *
     * @param document 文档任务
     * @return 文档读模型
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private Map<String, Object> documentView(DocumentJob document) {
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
     * 构建 reconciliation 批次视图。
     *
     * @param batch 批次聚合
     * @param documents 文档视图
     * @return 批次 reconciliation 视图
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private Map<String, Object> reconciliationBatchView(Batch batch, List<Map<String, Object>> documents) {
        Map<String, Object> payload = new LinkedHashMap<>(8);
        payload.put(BATCH_ID_FIELD, batch.batchId());
        payload.put(IDEMPOTENCY_KEY_FIELD, batch.idempotencyKey().orElse(EMPTY_VALUE));
        payload.put(STATUS_FIELD, batchStatus(batch));
        payload.put(TOTAL_FILES_FIELD, batch.totalFiles());
        payload.put(COMPLETED_FILES_FIELD, batch.completedFiles());
        payload.put(FAILED_FILES_FIELD, batch.failedFiles());
        payload.put(PROGRESS_PERCENT_FIELD, batchProgress(batch));
        payload.put(DOCUMENTS_FIELD, documents);
        payload.put(CREATED_AT_FIELD, batch.createdAt().toString());
        payload.put(UPDATED_AT_FIELD, batch.updatedAt().toString());
        return payload;
    }

    /**
     * 构建 reconciliation 文档视图。
     *
     * @param document 文档任务
     * @return 文档 reconciliation 视图
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private Map<String, Object> reconciliationDocumentView(DocumentJob document) {
        Map<String, Object> payload = new LinkedHashMap<>(11);
        payload.put(DOCUMENT_ID_FIELD, document.documentId());
        payload.put(FILE_NAME_FIELD, document.fileName());
        payload.put(STATUS_FIELD, documentStatus(document));
        payload.put("stage", reconciliationStage(document));
        payload.put(PROGRESS_PERCENT_FIELD, document.progressPercent());
        payload.put(CURRENT_PAGE_FIELD, document.currentPage());
        payload.put(TOTAL_PAGES_FIELD, document.totalPages());
        payload.put(RESULT_ID_FIELD, document.resultId().orElse(EMPTY_VALUE));
        payload.put(ERROR_FIELD, reconciliationError(document));
        payload.put(CREATED_AT_FIELD, document.createdAt().toString());
        payload.put(UPDATED_AT_FIELD, document.updatedAt().toString());
        return payload;
    }

    /**
     * 将批次状态转换为稳定 reconciliation 状态。
     *
     * @param batch 批次聚合
     * @return reconciliation 状态
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private String batchStatus(Batch batch) {
        return switch (batch.status()) {
            case QUEUED -> "queued";
            case PROCESSING -> "processing";
            case COMPLETED -> "completed";
            case FAILED, PARTIAL_FAILED -> "failed";
        };
    }

    /**
     * 将文档状态转换为稳定 reconciliation 状态。
     *
     * @param document 文档任务
     * @return reconciliation 状态
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private String documentStatus(DocumentJob document) {
        return switch (document.status()) {
            case QUEUED -> "queued";
            case PROCESSING, STALLED -> "processing";
            case COMPLETED -> "completed";
            case FAILED -> "failed";
        };
    }

    /**
     * 将文档阶段转换为稳定 reconciliation 阶段。
     *
     * @param document 文档任务
     * @return reconciliation 阶段
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private String reconciliationStage(DocumentJob document) {
        return switch (document.stage()) {
            case QUEUED, OCR_QUEUED -> "queued";
            case WORD_TO_PDF, WORD_TO_PDF_COMPLETED, PDF_TO_IMAGES, PDF_TO_IMAGES_COMPLETED, RENDERING ->
                    "extracting";
            case OCR_IMAGES, OCR_PROCESSING -> "ocr";
            case DIRECT_TEXT_SAVED, MERGE_TEXT, SAVE_TEXT, NORMALIZING -> "markdown";
            case COMPLETED, OCR_COMPLETED -> "completed";
            case FAILED, OCR_FAILED -> "failed";
        };
    }

    /**
     * 将文档错误转换为 reconciliation 错误对象。
     *
     * @param document 文档任务
     * @return reconciliation 错误对象
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private Map<String, Object> reconciliationError(DocumentJob document) {
        return Map.of(
                CODE_FIELD, document.errorCode().orElse(EMPTY_VALUE),
                MESSAGE_FIELD, document.errorMessage().orElse(EMPTY_VALUE)
        );
    }

    /**
     * 获取 OCR 结果读模型。
     *
     * @param documentId 文档 ID
     * @return 结果读模型
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public Map<String, Object> getDocumentResult(String documentId) {
        OcrResult result = resultRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("document result " + documentId + " not found"));
        return documentResultView(documentId, result);
    }

    /**
     * 获取 caller 范围内 OCR 结果读模型。
     *
     * @param caller caller 身份
     * @param documentId 文档 ID
     * @return 结果读模型
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public Map<String, Object> getDocumentResult(CallerIdentity caller, String documentId) {
        findCallerDocument(caller, documentId);
        OcrResult result = resultRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("document result " + documentId + " not found"));
        return documentResultView(documentId, result);
    }

    /**
     * 构建 OCR 结果读模型。
     *
     * @param documentId 文档 ID
     * @param result OCR 结果
     * @return 结果读模型
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private Map<String, Object> documentResultView(String documentId, OcrResult result) {
        Map<String, Object> summary = Map.of(
                "pageCount", result.pageText().size(),
                "blockCount", result.layoutBlocks().size(),
                "tableCount", result.tables().size(),
                "confidence", result.confidence()
        );
        Map<String, Object> payload = Map.ofEntries(
                Map.entry("finalText", result.finalText()),
                Map.entry("llm_markdown_applied", llmMarkdownApplied(result)),
                Map.entry("llm_error_message", llmErrorMessage(result)),
                Map.entry("markdownStorageUri", result.markdownStorageUri()),
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
     * 判断当前结果是否实际应用了 LLM Markdown 排版。
     *
     * @param result OCR 结果
     * @return 是否已应用 LLM Markdown
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private boolean llmMarkdownApplied(OcrResult result) {
        Object rawFlag = result.rawVendorOutput().get("llm_markdown_applied");
        return rawFlag instanceof Boolean applied && applied;
    }

    /**
     * 获取 LLM 回退时的失败原因。
     *
     * @param result OCR 结果
     * @return 脱敏后的失败原因
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private String llmErrorMessage(OcrResult result) {
        Object rawMessage = result.rawVendorOutput().get("llm_error_message");
        // 原始结果里有明确失败信息时直接返回。
        if (rawMessage instanceof String message && !message.isBlank()) {
            return message;
        } else {
            // 原始结果没有失败信息时返回空值，避免把 null 继续向上抛。
            return DocLensConstants.EMPTY_VALUE;
        }
    }

    /**
     * 获取批次事件时间线。
     *
     * @param batchId 批次 ID
     * @return 事件时间线
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public Map<String, Object> getEvents(String batchId) {
        List<Map<String, Object>> events = eventRepository.listByBatchId(batchId).stream().map(this::eventView).toList();
        return Map.of("batch_id", batchId, "events", events);
    }

    /**
     * 获取 caller 范围内批次事件时间线。
     *
     * @param caller caller 身份
     * @param batchId 批次 ID
     * @return 事件时间线
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public Map<String, Object> getEvents(CallerIdentity caller, String batchId) {
        Batch batch = findCallerBatch(caller, batchId);
        List<Map<String, Object>> events = eventRepository.listByBatchId(batch.batchId()).stream()
                .map(this::eventView)
                .toList();
        return Map.of("batch_id", batch.batchId(), "events", events);
    }

    /**
     * 查找 caller 范围内批次。
     *
     * @param caller caller 身份
     * @param batchId 批次 ID
     * @return 批次聚合
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private Batch findCallerBatch(CallerIdentity caller, String batchId) {
        return batchRepository.findByIdForCaller(caller, batchId)
                .orElseThrow(() -> new ResourceNotFoundException("batch " + batchId + " not found"));
    }

    /**
     * 查找 caller 范围内文档。
     *
     * @param caller caller 身份
     * @param documentId 文档 ID
     * @return 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private DocumentJob findCallerDocument(CallerIdentity caller, String documentId) {
        DocumentJob document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("document " + documentId + " not found"));
        findCallerBatch(caller, document.batchId());
        return document;
    }

    /**
     * 构建批次读模型。
     *
     * @param batch 批次聚合
     * @return 批次读模型
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private Map<String, Object> batchView(Batch batch) {
        return Map.ofEntries(
                Map.entry(BATCH_ID_FIELD, batch.batchId()),
                Map.entry(STATUS_FIELD, batch.status().name().toLowerCase(Locale.ROOT)),
                Map.entry(TOTAL_FILES_FIELD, batch.totalFiles()),
                Map.entry(COMPLETED_FILES_FIELD, batch.completedFiles()),
                Map.entry(FAILED_FILES_FIELD, batch.failedFiles()),
                Map.entry(PROGRESS_PERCENT_FIELD, batchProgress(batch)),
                Map.entry("metadata", batch.metadata().values()),
                Map.entry(CREATED_AT_FIELD, batch.createdAt().toString()),
                Map.entry(UPDATED_AT_FIELD, batch.updatedAt().toString())
        );
    }

    private int batchProgress(Batch batch) {
        // 总文件数大于 0 时按完成与失败的文件数计算进度。
        if (batch.totalFiles() > 0) {
            return (int) Math.round((batch.completedFiles() + batch.failedFiles()) * 100.0 / batch.totalFiles());
        } else {
            // 没有文件时直接返回 0，避免除零并保持响应稳定。
            return 0;
        }
    }

    private Map<String, Object> documentError(DocumentJob document) {
        // 文档没有错误码时返回空对象，表示不存在业务错误。
        if (document.errorCode().isEmpty()) {
            return Map.of();
        } else {
            // 文档有错误码时返回稳定的错误结构，方便前端直接消费。
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
