package io.github.lvdaxianer.doclens.j.processing.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文档单页 OCR 结果。
 *
 * @param documentId 文档 ID
 * @param pageNo 页码
 * @param rawOutput 原始 OCR 输出
 * @param pageText 页面文本
 * @param layoutBlocks 版面块集合
 * @param confidence 置信度
 * @param warnings 警告集合
 * @param nodeId OCR 节点 ID
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record DocumentPageResult(
        String documentId,
        int pageNo,
        Map<String, Object> rawOutput,
        String pageText,
        List<Map<String, Object>> layoutBlocks,
        double confidence,
        List<String> warnings,
        String nodeId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
