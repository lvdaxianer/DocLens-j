package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;

/**
 * 卡死文档恢复服务依赖持有对象。
 *
 * @param documentRepository 文档仓储
 * @param batchRepository 批次仓储
 * @param eventRepository 事件仓储
 * @param eventFactory 事件工厂
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record StaleDocumentRecoveryDependencies(
        DocumentJobRepository documentRepository,
        BatchRepository batchRepository,
        OcrEventRepository eventRepository,
        OcrEventFactory eventFactory
) {
}
