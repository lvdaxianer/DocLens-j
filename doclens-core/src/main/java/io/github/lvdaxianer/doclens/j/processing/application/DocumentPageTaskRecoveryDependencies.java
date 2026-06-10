package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskRepository;

/**
 * 文档页任务恢复依赖集合。
 *
 * @param pageTaskRepository 页任务仓储
 * @param pageResultRepository 页结果仓储
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
public record DocumentPageTaskRecoveryDependencies(
        DocumentPageTaskRepository pageTaskRepository,
        DocumentPageResultRepository pageResultRepository
) {
}
