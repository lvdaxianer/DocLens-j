package io.github.lvdaxianer.doclens.j.ingestion.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;

/**
 * 批次启动恢复服务依赖集合。
 *
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
public record BatchStartupRecoveryDependencies(
        DocumentJobRepository documentRepository,
        BatchRepository batchRepository,
        DocumentPageTaskRepository pageTaskRepository,
        DocumentPageResultRepository pageResultRepository,
        OcrEventRepository eventRepository,
        OcrEventFactory eventFactory,
        BatchProcessingScheduler batchProcessingScheduler
) {
}
