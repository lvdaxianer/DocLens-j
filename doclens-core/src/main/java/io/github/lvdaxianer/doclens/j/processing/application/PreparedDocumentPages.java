package io.github.lvdaxianer.doclens.j.processing.application;

import java.util.List;

/**
 * 文档页图片准备结果。
 *
 * @param batchId 批次 ID
 * @param documentId 文档 ID
 * @param fileName 文件名
 * @param pageImages 有序页图片引用
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record PreparedDocumentPages(
        String batchId,
        String documentId,
        String fileName,
        List<PageImageRef> pageImages
) {
}
