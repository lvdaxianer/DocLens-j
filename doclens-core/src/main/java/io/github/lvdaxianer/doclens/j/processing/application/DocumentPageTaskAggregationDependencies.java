package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;

/**
 * 文档页任务聚合依赖集合。
 *
 * @param documentRepository 文档仓储
 * @param pageTaskRepository 页任务仓储
 * @param pageResultRepository 页结果仓储
 * @param resultRepository OCR 结果仓储
 * @param objectStorage 对象存储
 * @param idGenerator ID 生成器
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
public record DocumentPageTaskAggregationDependencies(
        DocumentJobRepository documentRepository,
        DocumentPageTaskRepository pageTaskRepository,
        DocumentPageResultRepository pageResultRepository,
        OcrResultRepository resultRepository,
        ObjectStorage objectStorage,
        IdGenerator idGenerator
) {
}
