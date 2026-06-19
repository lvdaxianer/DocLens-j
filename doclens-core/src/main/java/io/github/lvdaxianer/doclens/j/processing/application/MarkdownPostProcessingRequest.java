package io.github.lvdaxianer.doclens.j.processing.application;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Markdown 后处理请求。
 *
 * @param documentId 文档 ID
 * @param fileName 文件名
 * @param metadata 上传元数据
 * @param ocrText OCR 合并文本
 * @param chunkStrategy 分块策略
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record MarkdownPostProcessingRequest(
        String documentId,
        String fileName,
        Map<String, Object> metadata,
        String ocrText,
        ChunkStrategy chunkStrategy
) {

    /**
     * 规整请求中的可空集合和文本。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public MarkdownPostProcessingRequest {
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
        ocrText = ocrText == null ? "" : ocrText;
        chunkStrategy = chunkStrategy == null ? ChunkStrategy.general() : chunkStrategy;
    }

    /**
     * 创建使用通用分块策略的后处理请求。
     *
     * @param documentId 文档 ID
     * @param fileName 文件名
     * @param metadata 上传元数据
     * @param ocrText OCR 合并文本
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    public MarkdownPostProcessingRequest(
            String documentId,
            String fileName,
            Map<String, Object> metadata,
            String ocrText
    ) {
        this(documentId, fileName, metadata, ocrText, ChunkStrategy.general());
    }
}
