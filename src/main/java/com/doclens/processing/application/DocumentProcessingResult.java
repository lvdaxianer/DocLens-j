package com.doclens.processing.application;

import com.doclens.processing.domain.DocumentJob;
import com.doclens.processing.domain.OcrEvent;
import com.doclens.processing.domain.OcrResult;
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
