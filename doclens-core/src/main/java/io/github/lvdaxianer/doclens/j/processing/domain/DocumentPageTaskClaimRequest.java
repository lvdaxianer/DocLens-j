package io.github.lvdaxianer.doclens.j.processing.domain;

import java.time.OffsetDateTime;

/**
 * 文档页任务原子抢占请求。
 *
 * @param taskId 页任务 ID
 * @param workerId 工作线程 ID
 * @param lockedUntil 锁过期时间
 * @param now 当前更新时间
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record DocumentPageTaskClaimRequest(
        String taskId,
        String workerId,
        OffsetDateTime lockedUntil,
        OffsetDateTime now
) {
}
