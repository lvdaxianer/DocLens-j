package io.github.lvdaxianer.doclens.j.processing.domain;

/**
 * 文档处理阶段。
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
