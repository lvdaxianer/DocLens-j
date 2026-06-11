package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCaseInfrastructure.InlineTransactionRunner;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCaseInfrastructure.RecordingObjectStorage;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.PdfMode;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 文档删除测试共享构造器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class DocumentDeleteUseCaseTestSupport {

    /** 测试批次 ID。 */
    static final String BATCH_ID = "batch-test";
    /** 测试文档字节数。 */
    private static final int DOCUMENT_SIZE_BYTES = 5;
    /** 测试文档页数。 */
    private static final int DOCUMENT_PAGE_COUNT = 1;
    /** 测试 OCR 引擎。 */
    private static final String OCR_ENGINE = "stub_ocr";
    /** 卡死文档进度百分比。 */
    private static final int STALLED_PROGRESS_PERCENT = 50;
    /** 测试事件进度百分比。 */
    private static final int EVENT_PROGRESS_PERCENT = 100;
    /** OCR 结果置信度。 */
    private static final double OCR_CONFIDENCE = 1.0;

    /**
     * 禁止实例化工具类。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentDeleteUseCaseTestSupport() {
    }

    /**
     * 创建待测文档删除用例。
     *
     * @param repositories 测试仓储集合
     * @param objectStorage 对象存储桩
     * @return 文档删除用例
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static DocumentDeleteUseCase documentUseCase(
            DocumentDeleteUseCaseRepositories repositories,
            RecordingObjectStorage objectStorage
    ) {
        return new DocumentDeleteUseCase(dependencies(repositories, objectStorage), new InlineTransactionRunner());
    }

    /**
     * 创建待测批次删除用例。
     *
     * @param repositories 测试仓储集合
     * @param objectStorage 对象存储桩
     * @return 批次删除用例
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static BatchDeleteUseCase batchUseCase(
            DocumentDeleteUseCaseRepositories repositories,
            RecordingObjectStorage objectStorage
    ) {
        return new BatchDeleteUseCase(dependencies(repositories, objectStorage), new InlineTransactionRunner());
    }

    /**
     * 创建测试文档。
     *
     * @param documentId 文档 ID
     * @param sortOrder 排序
     * @return 测试文档
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static DocumentJob document(String documentId, int sortOrder) {
        return DocumentJob.create(new DocumentJobCreateRequest(documentId, BATCH_ID, documentId + ".txt",
                DocumentType.TEXT, DOCUMENT_SIZE_BYTES, DOCUMENT_PAGE_COUNT, "local://" + documentId, OCR_ENGINE,
                Optional.empty(), JsonPayload.empty(), sortOrder, OffsetDateTime.now()));
    }

    /**
     * 创建卡死文档。
     *
     * @param documentId 文档 ID
     * @param now 测试时间
     * @param sortOrder 排序
     * @return 卡死文档
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static DocumentJob stalledDocument(String documentId, OffsetDateTime now, int sortOrder) {
        return new DocumentJob(documentId, BATCH_ID, documentId + ".txt", DocumentType.TEXT, DOCUMENT_SIZE_BYTES,
                DOCUMENT_PAGE_COUNT, "local://" + documentId, DocumentStatus.STALLED, ProcessingStage.OCR_IMAGES,
                STALLED_PROGRESS_PERCENT, DOCUMENT_PAGE_COUNT, DOCUMENT_PAGE_COUNT, OCR_ENGINE,
                Optional.<PdfMode>empty(), OcrRoutePolicy.defaultPolicy(), JsonPayload.empty(), Optional.empty(),
                Optional.of("STALE_DOCUMENT"), Optional.of("stalled"), sortOrder, now, now.plusSeconds(10));
    }

    /**
     * 创建已完成批次。
     *
     * @param totalFiles 总文件数
     * @param completedFiles 已完成文件数
     * @param failedFiles 失败文件数
     * @return 已完成批次
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static Batch completedBatch(int totalFiles, int completedFiles, int failedFiles) {
        return batchWithStatus(BatchStatus.COMPLETED, batchSummary(totalFiles, completedFiles, failedFiles));
    }

    /**
     * 创建处理中批次。
     *
     * @param totalFiles 总文件数
     * @param completedFiles 已完成文件数
     * @param failedFiles 失败文件数
     * @return 处理中批次
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static Batch processingBatch(int totalFiles, int completedFiles, int failedFiles) {
        return batchWithStatus(BatchStatus.PROCESSING, batchSummary(totalFiles, completedFiles, failedFiles));
    }

    /**
     * 创建部分失败批次。
     *
     * @param totalFiles 总文件数
     * @param completedFiles 已完成文件数
     * @param failedFiles 失败文件数
     * @return 部分失败批次
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static Batch failedBatch(int totalFiles, int completedFiles, int failedFiles) {
        return batchWithStatus(BatchStatus.PARTIAL_FAILED, batchSummary(totalFiles, completedFiles, failedFiles));
    }

    /**
     * 创建测试 OCR 结果。
     *
     * @param documentId 文档 ID
     * @param markdownStorageUri Markdown 存储地址
     * @return OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static OcrResult result(String documentId, String markdownStorageUri) {
        return new OcrResult("result-" + documentId, documentId, "text", markdownStorageUri, Map.of(), Map.of(),
                List.of(), List.of(), List.of(), List.of(), OCR_CONFIDENCE, List.of(), OffsetDateTime.now());
    }

    /**
     * 创建测试文档事件。
     *
     * @param documentId 文档 ID
     * @param eventType 事件类型
     * @return 文档事件
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static OcrEvent documentEvent(String documentId, String eventType) {
        return new OcrEvent("event-" + documentId + '-' + eventType, eventType, BATCH_ID, Optional.of(documentId),
                "failed", "ocr_images", Map.of("percent", EVENT_PROGRESS_PERCENT), JsonPayload.empty(),
                Optional.empty(), Map.of(), Map.of(), OffsetDateTime.now());
    }

    /**
     * 创建删除依赖集合。
     *
     * @param repositories 测试仓储集合
     * @param objectStorage 对象存储桩
     * @return 删除依赖集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static DocumentDeleteDependencies dependencies(
            DocumentDeleteUseCaseRepositories repositories,
            RecordingObjectStorage objectStorage
    ) {
        return new DocumentDeleteDependencies(repositories.documentRepository, repositories.batchRepository,
                repositories.resultRepository, repositories.eventRepository, objectStorage,
                new OcrEventFactory(new IdGenerator()));
    }

    /**
     * 创建批次摘要对象。
     *
     * @param totalFiles 总文件数
     * @param completedFiles 已完成文件数
     * @param failedFiles 失败文件数
     * @return 批次摘要
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static BatchSummary batchSummary(int totalFiles, int completedFiles, int failedFiles) {
        return new BatchSummary(totalFiles, completedFiles, failedFiles);
    }

    /**
     * 按状态创建测试批次。
     *
     * @param status 批次状态
     * @param summary 批次摘要
     * @return 测试批次
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static Batch batchWithStatus(BatchStatus status, BatchSummary summary) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Batch(BATCH_ID, status, summary.totalFiles(), summary.completedFiles(), summary.failedFiles(),
                Optional.empty(), Optional.empty(), status.name().toLowerCase(), JsonPayload.empty(), Optional.empty(),
                Optional.empty(), now, now);
    }

    /**
     * 批次摘要参数对象。
     *
     * @param totalFiles 总文件数
     * @param completedFiles 已完成文件数
     * @param failedFiles 失败文件数
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private record BatchSummary(int totalFiles, int completedFiles, int failedFiles) {
    }
}
