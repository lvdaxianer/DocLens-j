package io.github.lvdaxianer.doclens.j.adapter.application;

/**
 * OCR 批次运行中节点命中计数命令。
 *
 * @param batchId 批次 ID
 * @param documentId 文档 ID
 * @param modelKey 模型标识
 * @param nodeId 节点 ID
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
public record OcrBatchNodeHitCommand(
        String batchId,
        String documentId,
        String modelKey,
        String nodeId
) {
}
