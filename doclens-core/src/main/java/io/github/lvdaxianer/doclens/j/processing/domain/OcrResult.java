package io.github.lvdaxianer.doclens.j.processing.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * OCR 结构化结果聚合。
 *
 * @param resultId 结果 ID
 * @param documentId 文档 ID
 * @param rawVendorOutput 厂商原始输出
 * @param structuredDocument 归一化结构化文档
 * @param pageText 页面文本记录集合
 * @param layoutBlocks 版面块集合
 * @param tables 表格记录集合
 * @param images 图片记录集合
 * @param confidence OCR 置信度
 * @param warnings 归一化警告集合
 * @param createdAt 创建时间
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record OcrResult(
        String resultId,
        String documentId,
        Map<String, Object> rawVendorOutput,
        Map<String, Object> structuredDocument,
        List<Map<String, Object>> pageText,
        List<Map<String, Object>> layoutBlocks,
        List<Map<String, Object>> tables,
        List<Map<String, Object>> images,
        double confidence,
        List<String> warnings,
        OffsetDateTime createdAt
) {
}
