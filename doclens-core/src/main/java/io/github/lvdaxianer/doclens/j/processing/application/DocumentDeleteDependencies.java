package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;

/**
 * 文档删除用例依赖持有对象。
 *
 * @param documentRepository 文档仓储
 * @param batchRepository 批次仓储
 * @param resultRepository 结果仓储
 * @param eventRepository 事件仓储
 * @param objectStorage 对象存储
 * @param eventFactory 事件工厂
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record DocumentDeleteDependencies(
        DocumentJobRepository documentRepository,
        BatchRepository batchRepository,
        OcrResultRepository resultRepository,
        OcrEventRepository eventRepository,
        ObjectStorage objectStorage,
        OcrEventFactory eventFactory
) {
}
