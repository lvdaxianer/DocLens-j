package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.EventCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.domain.ResourceNotFoundException;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 批次级删除用例。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
public class BatchDeleteUseCase {

    private static final String DELETE_REASON = "manual_batch_delete";

    private final DocumentJobRepository documentRepository;
    private final BatchRepository batchRepository;
    private final OcrResultRepository resultRepository;
    private final OcrEventRepository eventRepository;
    private final ObjectStorage objectStorage;
    private final OcrEventFactory eventFactory;
    private final TransactionRunner transactionRunner;

    /**
     * 创建批次级删除用例。
     *
     * @param dependencies 删除依赖
     * @param transactionRunner 事务执行器
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    public BatchDeleteUseCase(
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
     * 删除批次下所有可删除文档，并在最后一个文档删除后清理空批次。
     *
     * @param batchId 批次 ID
     * @return 已删除文档数量
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    public int delete(String batchId) {
        return transactionRunner.requiredResult(() -> deleteWithinTransaction(batchId));
    }

    /**
     * 在事务内删除批次下所有可删除文档。
     *
     * @param batchId 批次 ID
     * @return 已删除文档数量
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private int deleteWithinTransaction(String batchId) {
        Batch batch = loadBatch(batchId);
        List<DocumentJob> documents = checkedDocuments(batchId);
        List<String> documentIds = documentIds(documents);
        List<OcrResult> results = resultRepository.findByDocumentIds(documentIds);
        objectStorage.deleteAll(storageUris(documents, results));
        resultRepository.deleteByDocumentIds(documentIds);
        eventRepository.deleteByDocumentIds(documentIds);
        documentRepository.deleteByIds(documentIds);
        eventRepository.saveAll(documents.stream()
                .map(document -> eventFactory.create(deleteEvent(batch, document)))
                .toList());
        batchRepository.deleteById(batchId);
        return documentIds.size();
    }

    /**
     * 加载待删除批次。
     *
     * @param batchId 批次 ID
     * @return 批次聚合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private Batch loadBatch(String batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("batch " + batchId + " not found"));
    }

    /**
     * 一次性读取并校验批次内文档，避免批次删除出现半完成状态。
     *
     * @param batchId 批次 ID
     * @return 批次内文档集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private List<DocumentJob> checkedDocuments(String batchId) {
        List<DocumentJob> documents = documentRepository.listByBatchId(batchId);
        if (documents.isEmpty()) {
            // 批次不存在或已被清空时返回资源不存在。
            throw new ResourceNotFoundException("batch " + batchId + " not found");
        } else {
            // 批次存在时先完成整批状态预校验，再返回文档快照。
            validateDocuments(documents);
            return documents;
        }
    }

    /**
     * 预校验整批文档状态，避免删除到一半才发现不可删除文档。
     *
     * @param documents 批次内文档集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void validateDocuments(List<DocumentJob> documents) {
        documents.forEach(document -> {
            if (isDeletable(document.status())) {
                // 可删除状态继续参与批次删除。
            } else {
                throw new IllegalArgumentException("batch contains non-deletable document "
                        + document.documentId() + " status " + document.status().name().toLowerCase());
            }
        });
    }

    /**
     * 判断文档状态是否允许批次删除。
     *
     * @param status 文档状态
     * @return 是否允许删除
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private boolean isDeletable(DocumentStatus status) {
        return status == DocumentStatus.FAILED || status == DocumentStatus.COMPLETED || status == DocumentStatus.STALLED;
    }

    /**
     * 提取批次内文档 ID 快照。
     *
     * @param documents 批次内文档集合
     * @return 文档 ID 集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private List<String> documentIds(List<DocumentJob> documents) {
        List<String> documentIds = new ArrayList<>(documents.size());
        documents.forEach(document -> documentIds.add(document.documentId()));
        return documentIds;
    }

    /**
     * 汇总批次删除涉及的所有对象存储 URI。
     *
     * @param documents 批次内文档集合
     * @param results 批次内 OCR 结果集合
     * @return 待删除 URI 集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private List<String> storageUris(List<DocumentJob> documents, List<OcrResult> results) {
        LinkedHashSet<String> storageUris = new LinkedHashSet<>(documents.size() + results.size());
        documents.forEach(document -> addStorageUri(storageUris, document.storageUri()));
        results.forEach(result -> addStorageUri(storageUris, result.markdownStorageUri()));
        return List.copyOf(storageUris);
    }

    /**
     * 收集非空存储 URI，避免重复删除同一对象。
     *
     * @param storageUris 存储 URI 集合
     * @param storageUri 候选存储 URI
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void addStorageUri(LinkedHashSet<String> storageUris, String storageUri) {
        if (storageUri != null && !storageUri.isBlank()) {
            storageUris.add(storageUri);
        } else {
            // 空 URI 不参与对象清理。
        }
    }

    /**
     * 构建批次删除事件。
     *
     * @param batch 批次聚合
     * @param document 已删除文档快照
     * @return 事件创建请求
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private EventCreateRequest deleteEvent(Batch batch, DocumentJob document) {
        return new EventCreateRequest(batch.batchId(), Optional.of(document.documentId()),
                io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants.EVENT_DOCUMENT_DELETED,
                document.status().name().toLowerCase(), document.stage().name().toLowerCase(),
                Map.of("percent", document.progressPercent(), "current_page", document.currentPage(),
                        "total_pages", document.totalPages()),
                document.metadata(), document.resultId(), Map.of("reason", DELETE_REASON), Map.of());
    }
}
