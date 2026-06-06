package com.doclens.processing.application;

import com.doclens.adapter.infrastructure.DefaultAdapterRegistry;
import com.doclens.ingestion.domain.BatchRepository;
import com.doclens.processing.domain.DocumentJobRepository;
import com.doclens.processing.domain.OcrEventFactory;
import com.doclens.processing.domain.OcrEventRepository;
import com.doclens.processing.domain.OcrResultRepository;
import com.doclens.shared.infrastructure.IdGenerator;
import org.springframework.stereotype.Component;

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
@Component
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
