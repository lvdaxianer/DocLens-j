package io.github.lvdaxianer.doclens.j.query.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrRuntimeNodePool;

/**
 * OCR Dashboard 运行态依赖集合。
 *
 * @param nodePool 运行时节点池
 * @param threadPools Dashboard 线程池集合
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
public record OcrDashboardRuntimeSources(
        OcrRuntimeNodePool nodePool,
        DashboardThreadPools threadPools
) {
}
