package io.github.lvdaxianer.doclens.j.adapter.application;

import java.util.List;

/**
 * OCR 运行中图片页任务追踪器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
public interface OcrRunningPageTaskTracker {

    /**
     * 记录图片页任务开始被 worker 线程消费。
     *
     * @param command 运行中图片页任务命令
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    void recordStart(OcrRunningPageTaskCommand command);

    /**
     * 记录图片页任务实际分配到的 OCR 节点。
     *
     * @param assignment OCR 节点分配
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    void recordAssignment(OcrRunningPageTaskAssignment assignment);

    /**
     * 记录图片页任务结束，移除运行中快照。
     *
     * @param taskId 页任务 ID
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    void recordCompletion(String taskId);

    /**
     * 获取指定批次当前运行中的图片页任务快照。
     *
     * @param batchId 批次 ID
     * @return 运行中图片页任务集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    List<OcrRunningPageTask> snapshotByBatch(String batchId);
}
