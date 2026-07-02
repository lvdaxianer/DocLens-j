package io.github.lvdaxianer.doclens.j.autoconfigure;

import io.github.lvdaxianer.doclens.j.adapter.domain.DefaultAdapterRegistry;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentPageTaskPreparationService;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractor;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;

/**
 * 批次处理 Bean 自动装配依赖。
 *
 * @param documentRepository 文档仓储
 * @param resultRepository 结果仓储
 * @param eventRepository 事件仓储
 * @param batchRepository 批次仓储
 * @param adapterRegistry 适配器注册表
 * @param objectStorage 对象存储
 * @param documentTextExtractor 文档文本提取器
 * @param idGenerator ID 生成器
 * @param eventFactory 事件工厂
 * @param markdownPostProcessor Markdown 后处理器
 * @param pageTaskPreparationService 页任务预处理服务
 * @param callbackJobRepository 回调任务仓储
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
record BatchProcessingBeanDependencies(
        DocumentJobRepository documentRepository,
        OcrResultRepository resultRepository,
        OcrEventRepository eventRepository,
        BatchRepository batchRepository,
        DefaultAdapterRegistry adapterRegistry,
        ObjectStorage objectStorage,
        DocumentTextExtractor documentTextExtractor,
        IdGenerator idGenerator,
        OcrEventFactory eventFactory,
        MarkdownPostProcessor markdownPostProcessor,
        DocumentPageTaskPreparationService pageTaskPreparationService,
        CallbackJobRepository callbackJobRepository
) {
}
