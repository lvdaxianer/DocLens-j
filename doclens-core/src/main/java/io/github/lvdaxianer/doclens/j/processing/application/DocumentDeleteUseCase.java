package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.EventCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.domain.ResourceNotFoundException;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 文档级删除用例。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public class DocumentDeleteUseCase {

    private static final String DELETE_REASON = "manual_delete";

    private final DocumentJobRepository documentRepository;
    private final BatchRepository batchRepository;
    private final OcrResultRepository resultRepository;
    private final OcrEventRepository eventRepository;
    private final ObjectStorage objectStorage;
    private final OcrEventFactory eventFactory;
    private final TransactionRunner transactionRunner;

    /**
     * 创建文档级删除用例。
     *
     * @param dependencies 删除依赖
     * @param transactionRunner 事务执行器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public DocumentDeleteUseCase(
            DocumentDeleteDependencies dependencies,
            TransactionRunner transactionRunner
    ) {
        this.documentRepository = dependencies.documentRepository();
        this.batchRepository = dependencies.batchRepository();
        this.resultRepository = dependencies.resultRepository();
        this.eventRepository = dependencies.eventRepository();
        this.objectStorage = dependencies.objectStorage();
        this.eventFactory = dependencies.eventFactory();
        this.transactionRunner = transactionRunner;
    }

    /**
     * 删除指定文档及其关联结果、事件和对象存储。
     *
     * @param documentId 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public void delete(String documentId) {
        transactionRunner.requiredVoid(() -> deleteWithinTransaction(documentId));
    }

    /**
     * 在事务内执行文档删除与批次摘要刷新。
     *
     * @param documentId 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void deleteWithinTransaction(String documentId) {
        DocumentJob document = loadDocument(documentId);
        Batch batch = loadBatch(document.batchId());
        validateDeletable(document);
        OcrResult result = resultRepository.findByDocumentId(documentId).orElse(null);
        deleteStoredObjects(document, result);
        resultRepository.deleteByDocumentId(documentId);
        eventRepository.deleteByDocumentId(documentId);
        documentRepository.deleteById(documentId);
        eventRepository.save(eventFactory.create(deleteEvent(batch, document)));
        refreshBatchSummary(batch.batchId());
    }

    /**
     * 加载待删除文档。
     *
     * @param documentId 文档 ID
     * @return 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentJob loadDocument(String documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("document " + documentId + " not found"));
    }

    /**
     * 加载文档所属批次。
     *
     * @param batchId 批次 ID
     * @return 批次聚合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private Batch loadBatch(String batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("batch " + batchId + " not found"));
    }

    /**
     * 校验文档当前状态是否允许删除。
     *
     * @param document 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void validateDeletable(DocumentJob document) {
        if (document.status() == DocumentStatus.FAILED
                || document.status() == DocumentStatus.COMPLETED
                || document.status() == DocumentStatus.STALLED) {
            // 允许删除已失败、已完成和已卡死文档。
        } else {
            throw new IllegalArgumentException("document " + document.documentId()
                    + " is not deletable from status " + document.status().name().toLowerCase());
        }
    }

    /**
     * 删除文档原始文件与 Markdown 结果文件。
     *
     * @param document 文档任务
     * @param result OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void deleteStoredObjects(DocumentJob document, OcrResult result) {
        LinkedHashSet<String> storageUris = new LinkedHashSet<>(4);
        addStorageUri(storageUris, document.storageUri());
        if (result != null) {
            addStorageUri(storageUris, result.markdownStorageUri());
        } else {
            // 没有 OCR 结果时只清理原始上传对象。
        }
        storageUris.forEach(objectStorage::delete);
    }

    /**
     * 收集非空存储 URI，避免重复删除同一对象。
     *
     * @param storageUris 存储 URI 集合
     * @param storageUri 候选存储 URI
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void addStorageUri(LinkedHashSet<String> storageUris, String storageUri) {
        if (storageUri != null && !storageUri.isBlank()) {
            storageUris.add(storageUri);
        } else {
            // 空 URI 不参与对象清理。
        }
    }

    /**
     * 刷新批次摘要，保证删除后列表和统计口径一致。
     *
     * @param batchId 批次 ID
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void refreshBatchSummary(String batchId) {
        List<DocumentJob> documents = documentRepository.listByBatchId(batchId);
        int completedCount = Math.toIntExact(documents.stream()
                .filter(document -> document.status() == DocumentStatus.COMPLETED)
                .count());
        int failedCount = Math.toIntExact(documents.stream()
                .filter(document -> document.status() == DocumentStatus.FAILED)
                .count());
        batchRepository.updateSummary(batchId, documents.size(), completedCount, failedCount, batchStatus(documents));
    }

    /**
     * 计算删除后的批次状态。
     *
     * @param documents 批次内剩余文档
     * @return 批次状态
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private BatchStatus batchStatus(List<DocumentJob> documents) {
        if (documents.isEmpty()) {
            return BatchStatus.COMPLETED;
        }
        long queuedCount = documents.stream().filter(document -> document.status() == DocumentStatus.QUEUED).count();
        long processingCount = documents.stream()
                .filter(document -> document.status() == DocumentStatus.PROCESSING)
                .count();
        long completedCount = documents.stream()
                .filter(document -> document.status() == DocumentStatus.COMPLETED)
                .count();
        long failedCount = documents.stream().filter(document -> document.status() == DocumentStatus.FAILED).count();
        if (queuedCount > 0 || processingCount > 0) {
            return BatchStatus.PROCESSING;
        }
        if (failedCount > 0 && completedCount > 0) {
            return BatchStatus.PARTIAL_FAILED;
        }
        if (failedCount > 0) {
            return BatchStatus.FAILED;
        }
        return BatchStatus.COMPLETED;
    }

    /**
     * 构建文档删除事件。
     *
     * @param batch 批次聚合
     * @param document 已删除文档快照
     * @return 事件创建请求
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private EventCreateRequest deleteEvent(Batch batch, DocumentJob document) {
        return new EventCreateRequest(batch.batchId(), Optional.of(document.documentId()),
                DocLensConstants.EVENT_DOCUMENT_DELETED, document.status().name().toLowerCase(),
                document.stage().name().toLowerCase(),
                Map.of("percent", document.progressPercent(), "current_page", document.currentPage(),
                        "total_pages", document.totalPages()),
                document.metadata(), document.resultId(), Map.of("reason", DELETE_REASON), Map.of());
    }
}
