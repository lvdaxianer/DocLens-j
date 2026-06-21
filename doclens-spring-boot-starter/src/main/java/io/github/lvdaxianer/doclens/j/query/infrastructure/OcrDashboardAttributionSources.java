package io.github.lvdaxianer.doclens.j.query.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrBatchHitTracker;
import io.github.lvdaxianer.doclens.j.adapter.application.NoopOcrRunningPageTaskTracker;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRunningPageTaskTracker;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrModelRegistry;

/**
 * OCR Dashboard 路由归因依赖集合。
 *
 * @param batchHitTracker 批次运行时命中跟踪器
 * @param runningPageTaskTracker 运行中图片页任务追踪器
 * @param modelRegistry OCR 模型注册表
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
public record OcrDashboardAttributionSources(
        OcrBatchHitTracker batchHitTracker,
        OcrRunningPageTaskTracker runningPageTaskTracker,
        OcrModelRegistry modelRegistry
) {

    /**
     * 创建无运行中图片页任务追踪器的归因依赖集合，兼容旧测试和嵌入式装配。
     *
     * @param batchHitTracker 批次运行时命中跟踪器
     * @param modelRegistry OCR 模型注册表
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    public OcrDashboardAttributionSources(OcrBatchHitTracker batchHitTracker, OcrModelRegistry modelRegistry) {
        this(batchHitTracker, new NoopOcrRunningPageTaskTracker(), modelRegistry);
    }
}
