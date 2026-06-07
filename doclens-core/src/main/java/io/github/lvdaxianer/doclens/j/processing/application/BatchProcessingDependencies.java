package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.DefaultAdapterRegistry;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;

/**
 * Dependency holder for batch processing use case.
 *
 * @param documentRepository document repository
 * @param resultRepository OCR result repository
 * @param eventRepository OCR event repository
 * @param batchRepository batch repository
 * @param adapterRegistry adapter registry
 * @param idGenerator id generator
 * @param eventFactory OCR event factory
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
