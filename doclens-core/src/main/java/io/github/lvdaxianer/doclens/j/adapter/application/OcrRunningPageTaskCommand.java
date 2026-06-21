package io.github.lvdaxianer.doclens.j.adapter.application;

import java.time.OffsetDateTime;

/**
 * OCR 运行中图片页任务开始命令。
 *
 * @param taskId 页任务 ID
 * @param batchId 批次 ID
 * @param documentId 文档 ID
 * @param pageNo 页码
 * @param workerId worker 标识
 * @param threadName 线程名
 * @param startedAt 开始时间
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
public record OcrRunningPageTaskCommand(
        String taskId,
        String batchId,
        String documentId,
        int pageNo,
        String workerId,
        String threadName,
        OffsetDateTime startedAt
) {
}
