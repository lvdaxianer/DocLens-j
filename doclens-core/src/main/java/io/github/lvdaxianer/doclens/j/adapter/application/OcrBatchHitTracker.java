package io.github.lvdaxianer.doclens.j.adapter.application;

import java.util.List;

/**
 * 批次维度 OCR 命中节点运行时跟踪器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public interface OcrBatchHitTracker {

    /**
     * 记录某个批次已派发到指定节点的进行中图片。
     *
     * @param batchId 批次 ID
     * @param modelKey 模型标识
     * @param nodeId 节点 ID
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    void recordDispatch(String batchId, String modelKey, String nodeId);

    /**
     * 记录某个批次在指定节点上的图片已完成处理。
     *
     * @param batchId 批次 ID
     * @param modelKey 模型标识
     * @param nodeId 节点 ID
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    void recordCompletion(String batchId, String modelKey, String nodeId);

    /**
     * 返回指定批次当前仍在处理中的节点命中快照。
     *
     * @param batchId 批次 ID
     * @return 运行时命中节点快照
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    List<OcrBatchNodeHit> snapshotByBatch(String batchId);
}
