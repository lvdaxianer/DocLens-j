package io.github.lvdaxianer.doclens.j.processing.domain;

import java.time.OffsetDateTime;

/**
 * 文档页任务完成请求。
 *
 * @param taskId 页任务 ID
 * @param workerId 完成任务的工作线程 ID
 * @param now 当前完成时间
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record DocumentPageTaskCompletionRequest(
        String taskId,
        String workerId,
        OffsetDateTime now
) {
}
