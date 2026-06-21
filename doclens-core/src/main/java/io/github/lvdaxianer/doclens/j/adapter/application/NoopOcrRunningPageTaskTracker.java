package io.github.lvdaxianer.doclens.j.adapter.application;

import java.util.List;

/**
 * 空操作 OCR 运行中图片页任务追踪器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
public class NoopOcrRunningPageTaskTracker implements OcrRunningPageTaskTracker {

    /**
     * 忽略图片页任务开始事件。
     *
     * @param command 运行中图片页任务命令
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public void recordStart(OcrRunningPageTaskCommand command) {
        // 空实现用于兼容未启用 Dashboard 运行态观测的嵌入式场景。
    }

    /**
     * 忽略图片页任务节点分配事件。
     *
     * @param assignment OCR 节点分配
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public void recordAssignment(OcrRunningPageTaskAssignment assignment) {
        // 空实现用于兼容未启用 Dashboard 运行态观测的嵌入式场景。
    }

    /**
     * 忽略图片页任务完成事件。
     *
     * @param taskId 页任务 ID
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public void recordCompletion(String taskId) {
        // 空实现用于兼容未启用 Dashboard 运行态观测的嵌入式场景。
    }

    /**
     * 返回空运行中快照。
     *
     * @param batchId 批次 ID
     * @return 空集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public List<OcrRunningPageTask> snapshotByBatch(String batchId) {
        return List.of();
    }
}
