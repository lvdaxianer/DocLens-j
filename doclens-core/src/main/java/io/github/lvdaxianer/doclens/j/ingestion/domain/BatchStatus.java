package io.github.lvdaxianer.doclens.j.ingestion.domain;

/**
 * 批次生命周期状态。
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
