package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import java.util.List;
import java.util.Optional;

/**
 * Result of processing one document in memory.
 *
 * @param document final document state
 * @param result optional OCR result
 * @param events lifecycle events
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record DocumentProcessingResult(
        DocumentJob document,
        Optional<OcrResult> result,
        List<OcrEvent> events
) {
}
