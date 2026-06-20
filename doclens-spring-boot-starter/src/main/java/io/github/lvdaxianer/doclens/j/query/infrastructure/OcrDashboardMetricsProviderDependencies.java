package io.github.lvdaxianer.doclens.j.query.infrastructure;

/**
 * OCR Dashboard 指标提供器依赖集合。
 *
 * @param dataSources 查询侧仓储集合
 * @param runtimeSources 运行态依赖集合
 * @param attributionSources 路由归因依赖集合
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
public record OcrDashboardMetricsProviderDependencies(
        OcrDashboardDataSources dataSources,
        OcrDashboardRuntimeSources runtimeSources,
        OcrDashboardAttributionSources attributionSources
) {
}
