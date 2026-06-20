package io.github.lvdaxianer.doclens.j.query.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrBatchHitTracker;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrModelRegistry;

/**
 * OCR Dashboard 路由归因依赖集合。
 *
 * @param batchHitTracker 批次运行时命中跟踪器
 * @param modelRegistry OCR 模型注册表
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
public record OcrDashboardAttributionSources(
        OcrBatchHitTracker batchHitTracker,
        OcrModelRegistry modelRegistry
) {
}
