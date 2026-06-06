package com.doclens.ingestion.domain;

/**
 * Batch lifecycle status.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public enum BatchStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    PARTIAL_FAILED
}
