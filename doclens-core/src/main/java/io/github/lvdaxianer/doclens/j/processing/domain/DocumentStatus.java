package io.github.lvdaxianer.doclens.j.processing.domain;

/**
 * 文档任务生命周期状态。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public enum DocumentStatus {
    QUEUED,
    PROCESSING,
    STALLED,
    COMPLETED,
    FAILED
}
