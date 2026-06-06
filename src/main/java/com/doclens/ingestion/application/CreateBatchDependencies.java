package com.doclens.ingestion.application;

import com.doclens.ingestion.domain.BatchRepository;
import com.doclens.processing.application.BatchProcessingUseCase;
import com.doclens.processing.domain.DocumentJobRepository;
import com.doclens.processing.domain.OcrEventFactory;
import com.doclens.processing.domain.OcrEventRepository;
import com.doclens.shared.config.DocLensProperties;
import com.doclens.shared.infrastructure.IdGenerator;
import com.doclens.storage.ObjectStorage;
import org.springframework.stereotype.Component;

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
@Component
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
