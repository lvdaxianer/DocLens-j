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
     * @param command 运行中节点命中计数命令
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    void recordDispatch(OcrBatchNodeHitCommand command);

    /**
     * 记录某个批次在指定节点上的图片已完成处理。
     *
     * @param command 运行中节点命中计数命令
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    void recordCompletion(OcrBatchNodeHitCommand command);

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
