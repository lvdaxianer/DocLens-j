package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.ingestion.application.BatchProcessingScheduler;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;

/**
 * 文档重试用例依赖持有对象。
 *
 * @param documentRepository 文档仓储
 * @param batchRepository 批次仓储
 * @param eventRepository 事件仓储
 * @param batchProcessingScheduler 批次处理调度器
 * @param eventFactory 事件工厂
 * @param cleanupDependencies 页级清理依赖
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record DocumentRetryDependencies(
        DocumentJobRepository documentRepository,
        BatchRepository batchRepository,
        OcrEventRepository eventRepository,
        BatchProcessingScheduler batchProcessingScheduler,
        OcrEventFactory eventFactory,
        DocumentRetryCleanupDependencies cleanupDependencies
) {
}
