package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskRepository;

/**
 * 文档重试页级清理依赖持有对象。
 *
 * @param pageTaskRepository 页任务仓储
 * @param pageResultRepository 页结果仓储
 * @author lvdaxianerplus
 * @date 2026-06-19
 */
public record DocumentRetryCleanupDependencies(
        DocumentPageTaskRepository pageTaskRepository,
        DocumentPageResultRepository pageResultRepository
) {
}
