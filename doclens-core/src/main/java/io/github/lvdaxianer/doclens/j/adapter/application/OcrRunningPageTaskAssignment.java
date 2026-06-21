package io.github.lvdaxianer.doclens.j.adapter.application;

/**
 * OCR 运行中图片页任务节点分配。
 *
 * @param taskId 页任务 ID
 * @param modelKey OCR 模型标识
 * @param nodeId OCR 节点标识
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
public record OcrRunningPageTaskAssignment(
        String taskId,
        String modelKey,
        String nodeId
) {
}
