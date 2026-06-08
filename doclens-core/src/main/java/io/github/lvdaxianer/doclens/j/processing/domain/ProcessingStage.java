package io.github.lvdaxianer.doclens.j.processing.domain;

/**
 * 文档处理阶段。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public enum ProcessingStage {
    QUEUED,
    DIRECT_TEXT_SAVED,
    WORD_TO_PDF,
    WORD_TO_PDF_COMPLETED,
    PDF_TO_IMAGES,
    PDF_TO_IMAGES_COMPLETED,
    OCR_IMAGES,
    MERGE_TEXT,
    SAVE_TEXT,
    COMPLETED,
    FAILED,
    RENDERING,
    OCR_PROCESSING,
    NORMALIZING,
    OCR_COMPLETED,
    OCR_FAILED
}
