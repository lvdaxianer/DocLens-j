package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.DefaultAdapterRegistry;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;

/**
 * 批次处理用例的依赖持有对象。
 *
 * @param documentRepository 文档仓储
 * @param resultRepository OCR 结果仓储
 * @param eventRepository OCR 事件仓储
 * @param batchRepository 批次仓储
 * @param adapterRegistry 适配器注册表
 * @param idGenerator ID 生成器
 * @param eventFactory OCR 事件工厂
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record BatchProcessingDependencies(
        DocumentJobRepository documentRepository,
        OcrResultRepository resultRepository,
        OcrEventRepository eventRepository,
        BatchRepository batchRepository,
        DefaultAdapterRegistry adapterRegistry,
        IdGenerator idGenerator,
        OcrEventFactory eventFactory
) {
}
