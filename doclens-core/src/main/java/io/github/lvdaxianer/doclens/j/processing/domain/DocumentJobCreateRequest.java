package io.github.lvdaxianer.doclens.j.processing.domain;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.processing.application.ChunkStrategy;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 创建文档任务的请求对象。
 *
 * @param documentId 文档 ID
 * @param batchId 批次 ID
 * @param fileName 原始文件名
 * @param fileType 检测出的文件类型
 * @param fileSize 文件大小
 * @param pageCount 检测出的页数
 * @param storageUri 上传存储 URI
 * @param adapterName 适配器键
 * @param pdfMode PDF 处理模式
 * @param ocrRoutePolicy OCR 路由策略快照
 * @param metadata 元数据载荷
 * @param sortOrder 上传顺序
 * @param now 当前时间
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record DocumentJobCreateRequest(
        String documentId,
        String batchId,
        String fileName,
        DocumentType fileType,
        long fileSize,
        int pageCount,
        String storageUri,
        String adapterName,
        Optional<PdfMode> pdfMode,
        OcrRoutePolicy ocrRoutePolicy,
        JsonPayload metadata,
        int sortOrder,
        OffsetDateTime now,
        ChunkStrategy chunkStrategy
) {
    /**
     * 创建带安全默认值的文档任务请求。
     *
     * @param documentId 文档 ID
     * @param batchId 批次 ID
     * @param fileName 文件名
     * @param fileType 文件类型
     * @param fileSize 文件大小
     * @param pageCount 页数
     * @param storageUri 存储 URI
     * @param adapterName 适配器键
     * @param pdfMode 可选 PDF 模式
     * @param metadata 元数据载荷
     * @param sortOrder 上传顺序
     * @param now 当前时间
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public DocumentJobCreateRequest(
            String documentId,
            String batchId,
            String fileName,
            DocumentType fileType,
            long fileSize,
            int pageCount,
            String storageUri,
            String adapterName,
            Optional<PdfMode> pdfMode,
            JsonPayload metadata,
            int sortOrder,
            OffsetDateTime now
    ) {
        this(documentId, batchId, fileName, fileType, fileSize, pageCount, storageUri, adapterName, pdfMode,
                OcrRoutePolicy.defaultPolicy(), metadata, sortOrder, now, ChunkStrategy.general());
    }

    /**
     * 创建带安全默认值的文档任务请求。
     *
     * @param documentId 文档 ID
     * @param batchId 批次 ID
     * @param fileName 文件名
     * @param fileType 文件类型
     * @param fileSize 文件大小
     * @param pageCount 页数
     * @param storageUri 存储 URI
     * @param adapterName 适配器键
     * @param pdfMode 可选 PDF 模式
     * @param ocrRoutePolicy OCR 路由策略快照
     * @param metadata 元数据载荷
     * @param sortOrder 上传顺序
     * @param now 当前时间
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    public DocumentJobCreateRequest(
            String documentId,
            String batchId,
            String fileName,
            DocumentType fileType,
            long fileSize,
            int pageCount,
            String storageUri,
            String adapterName,
            Optional<PdfMode> pdfMode,
            OcrRoutePolicy ocrRoutePolicy,
            JsonPayload metadata,
            int sortOrder,
            OffsetDateTime now
    ) {
        this(documentId, batchId, fileName, fileType, fileSize, pageCount, storageUri, adapterName, pdfMode,
                ocrRoutePolicy, metadata, sortOrder, now, ChunkStrategy.general());
    }

    /**
     * 创建带安全默认值的文档任务请求。
     *
     * @param documentId 文档 ID
     * @param batchId 批次 ID
     * @param fileName 文件名
     * @param fileType 文件类型
     * @param fileSize 文件大小
     * @param pageCount 页数
     * @param storageUri 存储 URI
     * @param adapterName 适配器键
     * @param pdfMode 可选 PDF 模式
     * @param metadata 元数据载荷
     * @param sortOrder 上传顺序
     * @param now 当前时间
     * @param chunkStrategy 分块策略
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    public DocumentJobCreateRequest(
            String documentId,
            String batchId,
            String fileName,
            DocumentType fileType,
            long fileSize,
            int pageCount,
            String storageUri,
            String adapterName,
            Optional<PdfMode> pdfMode,
            JsonPayload metadata,
            int sortOrder,
            OffsetDateTime now,
            ChunkStrategy chunkStrategy
    ) {
        this(documentId, batchId, fileName, fileType, fileSize, pageCount, storageUri, adapterName, pdfMode,
                OcrRoutePolicy.defaultPolicy(), metadata, sortOrder, now, chunkStrategy);
    }

    /**
     * 创建带安全默认值的文档任务请求。
     *
     * @param documentId 文档 ID
     * @param batchId 批次 ID
     * @param fileName 文件名
     * @param fileType 文件类型
     * @param fileSize 文件大小
     * @param pageCount 页数
     * @param storageUri 存储 URI
     * @param adapterName 适配器键
     * @param pdfMode 可选 PDF 模式
     * @param ocrRoutePolicy OCR 路由策略快照
     * @param metadata 元数据载荷
     * @param sortOrder 上传顺序
     * @param now 当前时间
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public DocumentJobCreateRequest {
        pdfMode = pdfMode == null ? Optional.empty() : pdfMode;
        if (ocrRoutePolicy == null) {
            ocrRoutePolicy = OcrRoutePolicy.defaultPolicy();
        } else {
            // 调用方已指定 OCR 路由策略快照。
        }
        metadata = metadata == null ? JsonPayload.empty() : metadata;
        chunkStrategy = chunkStrategy == null ? ChunkStrategy.general() : chunkStrategy;
    }
}
