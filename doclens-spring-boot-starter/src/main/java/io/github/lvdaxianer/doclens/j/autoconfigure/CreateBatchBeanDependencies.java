package io.github.lvdaxianer.doclens.j.autoconfigure;

import io.github.lvdaxianer.doclens.j.ingestion.application.BatchProcessingScheduler;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;

/**
 * 批次创建 Bean 自动装配依赖。
 *
 * @param batchRepository 批次仓储
 * @param documentRepository 文档仓储
 * @param eventRepository 事件仓储
 * @param objectStorage 对象存储
 * @param idGenerator ID 生成器
 * @param properties 运行时属性
 * @param batchProcessingScheduler 批次处理调度器
 * @param eventFactory 事件工厂
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
record CreateBatchBeanDependencies(
        BatchRepository batchRepository,
        DocumentJobRepository documentRepository,
        OcrEventRepository eventRepository,
        ObjectStorage objectStorage,
        IdGenerator idGenerator,
        DocLensProperties properties,
        BatchProcessingScheduler batchProcessingScheduler,
        OcrEventFactory eventFactory
) {
}
