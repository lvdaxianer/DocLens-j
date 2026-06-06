package com.doclens.processing.domain;

/**
 * Document processing stage.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public enum ProcessingStage {
    QUEUED,
    RENDERING,
    OCR_PROCESSING,
    NORMALIZING,
    OCR_COMPLETED,
    OCR_FAILED
}
