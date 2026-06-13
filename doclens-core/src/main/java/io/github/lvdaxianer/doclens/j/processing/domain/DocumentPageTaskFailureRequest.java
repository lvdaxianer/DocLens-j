package io.github.lvdaxianer.doclens.j.processing.domain;

import java.time.OffsetDateTime;

/**
 * 文档页任务终态失败请求。
 *
 * @param taskId 页任务 ID
 * @param errorCode 错误码
 * @param errorMessage 错误信息
 * @param now 当前失败时间
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record DocumentPageTaskFailureRequest(
        String taskId,
        String errorCode,
        String errorMessage,
        OffsetDateTime now
) {
}
