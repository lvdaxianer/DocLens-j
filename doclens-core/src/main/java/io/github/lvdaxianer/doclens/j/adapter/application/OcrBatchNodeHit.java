package io.github.lvdaxianer.doclens.j.adapter.application;

/**
 * 批次维度 OCR 节点命中快照。
 *
 * @param batchId 批次 ID
 * @param modelKey 模型标识
 * @param nodeId 节点 ID
 * @param imageCount 当前仍在该节点处理中的图片数
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record OcrBatchNodeHit(
        String batchId,
        String modelKey,
        String nodeId,
        long imageCount
) {
}
