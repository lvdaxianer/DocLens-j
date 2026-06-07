package io.github.lvdaxianer.doclens.j.ingestion.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCase;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;

/**
 * Dependency holder for batch creation use case.
 *
 * @param batchRepository batch repository
 * @param documentRepository document repository
 * @param eventRepository event repository
 * @param objectStorage object storage
 * @param idGenerator id generator
 * @param properties runtime properties
 * @param batchProcessingUseCase batch processing use case
 * @param eventFactory OCR event factory
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record CreateBatchDependencies(
        BatchRepository batchRepository,
        DocumentJobRepository documentRepository,
        OcrEventRepository eventRepository,
        ObjectStorage objectStorage,
        IdGenerator idGenerator,
        DocLensProperties properties,
        BatchProcessingUseCase batchProcessingUseCase,
        OcrEventFactory eventFactory
) {
}
