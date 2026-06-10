package io.github.lvdaxianer.doclens.j.processing.domain;

/**
 * 文档页 OCR 任务状态。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public enum DocumentPageTaskStatus {

    /**
     * 等待 OCR 调度。
     */
    QUEUED,

    /**
     * 已被工作线程抢占处理。
     */
    PROCESSING,

    /**
     * OCR 已完成并已持久化页结果。
     */
    COMPLETED,

    /**
     * OCR 已达到终态失败。
     */
    FAILED
}
