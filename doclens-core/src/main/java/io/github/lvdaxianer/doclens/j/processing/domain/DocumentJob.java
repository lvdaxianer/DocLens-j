package io.github.lvdaxianer.doclens.j.processing.domain;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.processing.application.ChunkStrategy;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 文档任务聚合根。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record DocumentJob(
        String documentId,
        String batchId,
        String fileName,
        DocumentType fileType,
        long fileSize,
        int pageCount,
        String storageUri,
        DocumentStatus status,
        ProcessingStage stage,
        int progressPercent,
        int currentPage,
        int totalPages,
        String adapterName,
        Optional<PdfMode> pdfMode,
        ChunkStrategy chunkStrategy,
        OcrRoutePolicy ocrRoutePolicy,
        JsonPayload metadata,
        boolean llmOrchestrated,
        Optional<String> resultId,
        Optional<String> errorCode,
        Optional<String> errorMessage,
        int sortOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    private static final int OCR_PROGRESS_RANGE = DocLensConstants.MAX_PROCESSING_PROGRESS_PERCENT
            - DocLensConstants.START_PROGRESS_PERCENT;

    /**
     * 创建带安全可选默认值的文档任务。
     *
     * @param documentId 文档 ID
     * @param batchId 批次 ID
     * @param fileName 文件名
     * @param fileType 文件类型
     * @param fileSize 文件大小
     * @param pageCount 页数
     * @param storageUri 存储 URI
     * @param status 文档状态
     * @param stage 处理阶段
     * @param progressPercent 进度百分比
     * @param currentPage 当前页
     * @param totalPages 总页数
     * @param adapterName 适配器键
     * @param pdfMode 可选 PDF 模式
     * @param chunkStrategy 分块策略
     * @param ocrRoutePolicy OCR 路由策略快照
     * @param metadata 元数据载荷
     * @param llmOrchestrated 是否已由上游完成 LLM 编排
     * @param resultId 可选结果 ID
     * @param errorCode 可选错误码
     * @param errorMessage 可选错误消息
     * @param sortOrder 上传顺序
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public DocumentJob {
        pdfMode = pdfMode == null ? Optional.empty() : pdfMode;
        if (ocrRoutePolicy == null) {
            ocrRoutePolicy = OcrRoutePolicy.defaultPolicy();
        } else {
            // 调用方已提供文档级 OCR 路由策略快照。
        }
        metadata = metadata == null ? JsonPayload.empty() : metadata;
        resultId = resultId == null ? Optional.empty() : resultId;
        errorCode = errorCode == null ? Optional.empty() : errorCode;
        errorMessage = errorMessage == null ? Optional.empty() : errorMessage;
    }

    /**
     * 创建兼容旧调用方式的文档任务。
     *
     * @param documentId 文档 ID
     * @param batchId 批次 ID
     * @param fileName 文件名
     * @param fileType 文件类型
     * @param fileSize 文件大小
     * @param pageCount 页数
     * @param storageUri 存储 URI
     * @param status 文档状态
     * @param stage 处理阶段
     * @param progressPercent 进度百分比
     * @param currentPage 当前页
     * @param totalPages 总页数
     * @param adapterName 适配器键
     * @param pdfMode 可选 PDF 模式
     * @param ocrRoutePolicy OCR 路由策略快照
     * @param metadata 元数据载荷
     * @param resultId 可选结果 ID
     * @param errorCode 可选错误码
     * @param errorMessage 可选错误消息
     * @param sortOrder 上传顺序
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    public DocumentJob(
            String documentId,
            String batchId,
            String fileName,
            DocumentType fileType,
            long fileSize,
            int pageCount,
            String storageUri,
            DocumentStatus status,
            ProcessingStage stage,
            int progressPercent,
            int currentPage,
            int totalPages,
            String adapterName,
            Optional<PdfMode> pdfMode,
            OcrRoutePolicy ocrRoutePolicy,
            JsonPayload metadata,
            boolean llmOrchestrated,
            Optional<String> resultId,
            Optional<String> errorCode,
            Optional<String> errorMessage,
            int sortOrder,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(documentId, batchId, fileName, fileType, fileSize, pageCount, storageUri, status, stage, progressPercent,
                currentPage, totalPages, adapterName, pdfMode, ChunkStrategy.general(), ocrRoutePolicy, metadata,
                false, resultId, errorCode, errorMessage, sortOrder, createdAt, updatedAt);
    }

    /**
     * 创建兼容旧调用方式的文档任务。
     *
     * @param documentId 文档 ID
     * @param batchId 批次 ID
     * @param fileName 文件名
     * @param fileType 文件类型
     * @param fileSize 文件大小
     * @param pageCount 页数
     * @param storageUri 存储 URI
     * @param status 文档状态
     * @param stage 处理阶段
     * @param progressPercent 进度百分比
     * @param currentPage 当前页
     * @param totalPages 总页数
     * @param adapterName 适配器键
     * @param pdfMode 可选 PDF 模式
     * @param chunkStrategy 分块策略
     * @param ocrRoutePolicy OCR 路由策略快照
     * @param metadata 元数据载荷
     * @param resultId 可选结果 ID
     * @param errorCode 可选错误码
     * @param errorMessage 可选错误消息
     * @param sortOrder 上传顺序
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    public DocumentJob(
            String documentId,
            String batchId,
            String fileName,
            DocumentType fileType,
            long fileSize,
            int pageCount,
            String storageUri,
            DocumentStatus status,
            ProcessingStage stage,
            int progressPercent,
            int currentPage,
            int totalPages,
            String adapterName,
            Optional<PdfMode> pdfMode,
            ChunkStrategy chunkStrategy,
            OcrRoutePolicy ocrRoutePolicy,
            JsonPayload metadata,
            Optional<String> resultId,
            Optional<String> errorCode,
            Optional<String> errorMessage,
            int sortOrder,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(documentId, batchId, fileName, fileType, fileSize, pageCount, storageUri, status, stage, progressPercent,
                currentPage, totalPages, adapterName, pdfMode, chunkStrategy, ocrRoutePolicy, metadata, false,
                resultId, errorCode, errorMessage, sortOrder, createdAt, updatedAt);
    }

    /**
     * 创建兼容旧调用方式的文档任务。
     *
     * @param documentId 文档 ID
     * @param batchId 批次 ID
     * @param fileName 文件名
     * @param fileType 文件类型
     * @param fileSize 文件大小
     * @param pageCount 页数
     * @param storageUri 存储 URI
     * @param status 文档状态
     * @param stage 处理阶段
     * @param progressPercent 进度百分比
     * @param currentPage 当前页
     * @param totalPages 总页数
     * @param adapterName 适配器键
     * @param pdfMode 可选 PDF 模式
     * @param ocrRoutePolicy OCR 路由策略快照
     * @param metadata 元数据载荷
     * @param resultId 可选结果 ID
     * @param errorCode 可选错误码
     * @param errorMessage 可选错误消息
     * @param sortOrder 上传顺序
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    public DocumentJob(
            String documentId,
            String batchId,
            String fileName,
            DocumentType fileType,
            long fileSize,
            int pageCount,
            String storageUri,
            DocumentStatus status,
            ProcessingStage stage,
            int progressPercent,
            int currentPage,
            int totalPages,
            String adapterName,
            Optional<PdfMode> pdfMode,
            OcrRoutePolicy ocrRoutePolicy,
            JsonPayload metadata,
            Optional<String> resultId,
            Optional<String> errorCode,
            Optional<String> errorMessage,
            int sortOrder,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(documentId, batchId, fileName, fileType, fileSize, pageCount, storageUri, status, stage, progressPercent,
                currentPage, totalPages, adapterName, pdfMode, ChunkStrategy.general(), ocrRoutePolicy, metadata,
                false, resultId, errorCode, errorMessage, sortOrder, createdAt, updatedAt);
    }

    /**
     * 创建排队中的文档任务。
     *
     * @param request 文档创建请求
     * @return 排队中的文档任务
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public static DocumentJob create(DocumentJobCreateRequest request) {
        return new DocumentJob(
                request.documentId(), request.batchId(), request.fileName(), request.fileType(), request.fileSize(),
                request.pageCount(), request.storageUri(), DocumentStatus.QUEUED, ProcessingStage.QUEUED, 0, 0,
                request.pageCount(), request.adapterName(), request.pdfMode(), request.chunkStrategy(),
                request.ocrRoutePolicy(), request.metadata(), request.llmOrchestrated(), Optional.empty(),
                Optional.empty(), Optional.empty(), request.sortOrder(), request.now(), request.now()
        );
    }

    /**
     * 将文档标记为处理中。
     *
     * @param now 当前时间
     * @return 更新后的文档
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public DocumentJob startProcessing(OffsetDateTime now) {
        return withState(DocumentStatus.PROCESSING, ProcessingStage.OCR_IMAGES, DocLensConstants.START_PROGRESS_PERCENT, 0, totalPages, resultId,
                errorCode, errorMessage, now);
    }

    /**
     * 标记文档页图片已准备完成并等待页级 OCR 调度。
     *
     * @param nextTotalPages 总页数
     * @param now 当前时间
     * @return 等待页级 OCR 调度的文档
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public DocumentJob markOcrQueued(int nextTotalPages, OffsetDateTime now) {
        int safeTotalPages = Math.max(DocLensConstants.DEFAULT_PAGE_COUNT, nextTotalPages);
        return withState(DocumentStatus.PROCESSING, ProcessingStage.OCR_QUEUED,
                DocLensConstants.START_PROGRESS_PERCENT, 0, safeTotalPages, resultId, Optional.empty(),
                Optional.empty(), now);
    }

    /**
     * 推进文档处理阶段和图片页进度。
     *
     * @param nextStage 下一阶段
     * @param nextCurrentPage 当前已完成图片页
     * @param nextTotalPages 总图片页
     * @param now 当前时间
     * @return 更新后的文档
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public DocumentJob advanceStage(
            ProcessingStage nextStage,
            int nextCurrentPage,
            int nextTotalPages,
            OffsetDateTime now
    ) {
        int safeTotalPages = Math.max(DocLensConstants.DEFAULT_PAGE_COUNT, nextTotalPages);
        int safeCurrentPage = Math.max(0, Math.min(nextCurrentPage, safeTotalPages));
        int percent = progressPercent(nextStage, safeCurrentPage, safeTotalPages);
        return withState(DocumentStatus.PROCESSING, nextStage, percent, safeCurrentPage, safeTotalPages, resultId,
                errorCode, errorMessage, now);
    }

    /**
     * 更新文档页进度。
     *
     * @param currentPage 当前已完成页
     * @param totalPages 总页数
     * @param now 当前时间
     * @return 更新后的文档
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public DocumentJob markPageCompleted(int currentPage, int totalPages, OffsetDateTime now) {
        int percent = pageProgressPercent(currentPage, totalPages);
        return withState(DocumentStatus.PROCESSING, ProcessingStage.OCR_IMAGES, percent, currentPage, totalPages,
                resultId, errorCode, errorMessage, now);
    }

    /**
     * 将文档标记为已完成。
     *
     * @param resultId OCR 结果 ID
     * @param now 当前时间
     * @return 更新后的文档
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public DocumentJob complete(String resultId, OffsetDateTime now) {
        return withState(DocumentStatus.COMPLETED, ProcessingStage.COMPLETED, DocLensConstants.COMPLETED_PROGRESS_PERCENT, totalPages, totalPages,
                Optional.of(resultId), Optional.empty(), Optional.empty(), now);
    }

    /**
     * 将文档标记为失败，并保留失败发生前的处理阶段。
     *
     * @param code 错误码
     * @param message 错误消息
     * @param now 当前时间
     * @return 更新后的文档
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public DocumentJob fail(String code, String message, OffsetDateTime now) {
        return withState(DocumentStatus.FAILED, stage, DocLensConstants.COMPLETED_PROGRESS_PERCENT, currentPage, totalPages, resultId,
                Optional.of(code), Optional.ofNullable(message), now);
    }

    /**
     * 将长时间无进展的处理中任务标记为卡住。
     *
     * @param code 错误码
     * @param message 错误消息
     * @param now 当前时间
     * @return 标记为卡住后的文档任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public DocumentJob stall(String code, String message, OffsetDateTime now) {
        return withState(DocumentStatus.STALLED, stage, progressPercent, currentPage, totalPages, resultId,
                Optional.of(code), Optional.ofNullable(message), now);
    }

    /**
     * 将失败或卡死文档重置为待重新调度状态。
     *
     * @param now 当前时间
     * @return 重置后的文档任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public DocumentJob retry(OffsetDateTime now) {
        return withState(DocumentStatus.QUEUED, ProcessingStage.QUEUED, DocLensConstants.ZERO_PROGRESS_PERCENT, 0,
                totalPages, Optional.empty(), Optional.empty(), Optional.empty(), now);
    }

    private int progressPercent(ProcessingStage nextStage, int nextCurrentPage, int nextTotalPages) {
        if (nextStage == ProcessingStage.COMPLETED) {
            return DocLensConstants.COMPLETED_PROGRESS_PERCENT;
        } else {
            return processingProgressPercent(nextStage, nextCurrentPage, nextTotalPages);
        }
    }

    private int processingProgressPercent(ProcessingStage nextStage, int nextCurrentPage, int nextTotalPages) {
        if (nextStage == ProcessingStage.OCR_IMAGES) {
            return pageProgressPercent(nextCurrentPage, nextTotalPages);
        } else {
            return Math.max(progressPercent, DocLensConstants.START_PROGRESS_PERCENT);
        }
    }

    /**
     * 按 OCR 页完成比例计算平滑进度。
     *
     * @param nextCurrentPage 当前已完成页
     * @param nextTotalPages 总页数
     * @return 处理中的进度百分比
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private int pageProgressPercent(int nextCurrentPage, int nextTotalPages) {
        int safeTotalPages = Math.max(DocLensConstants.DEFAULT_PAGE_COUNT, nextTotalPages);
        int safeCurrentPage = Math.max(0, Math.min(nextCurrentPage, safeTotalPages));
        int percent = DocLensConstants.START_PROGRESS_PERCENT
                + (int) Math.round(safeCurrentPage * (double) OCR_PROGRESS_RANGE / safeTotalPages);
        return Math.min(DocLensConstants.MAX_PROCESSING_PROGRESS_PERCENT,
                Math.max(DocLensConstants.START_PROGRESS_PERCENT, percent));
    }

    private DocumentJob withState(
            DocumentStatus nextStatus,
            ProcessingStage nextStage,
            int nextProgress,
            int nextCurrentPage,
            int nextTotalPages,
            Optional<String> nextResultId,
            Optional<String> nextErrorCode,
            Optional<String> nextErrorMessage,
            OffsetDateTime now
    ) {
        return new DocumentJob(documentId, batchId, fileName, fileType, fileSize, pageCount, storageUri, nextStatus,
                nextStage, nextProgress, nextCurrentPage, nextTotalPages, adapterName, pdfMode, chunkStrategy,
                ocrRoutePolicy, metadata, llmOrchestrated, nextResultId, nextErrorCode, nextErrorMessage, sortOrder,
                createdAt, now);
    }
}
