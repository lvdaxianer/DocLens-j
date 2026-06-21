package io.github.lvdaxianer.doclens.j.adapter.application;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * OCR 运行中图片页任务快照。
 *
 * @param taskId 页任务 ID
 * @param batchId 批次 ID
 * @param documentId 文档 ID
 * @param pageNo 页码
 * @param workerId worker 标识
 * @param threadName 线程名
 * @param startedAt 开始时间
 * @param modelKey OCR 模型标识
 * @param nodeId OCR 节点标识
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
public record OcrRunningPageTask(
        String taskId,
        String batchId,
        String documentId,
        int pageNo,
        String workerId,
        String threadName,
        OffsetDateTime startedAt,
        Optional<String> modelKey,
        Optional<String> nodeId
) {
}
