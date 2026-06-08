package io.github.lvdaxianer.doclens.j.ingestion.application;

/**
 * 批次处理调度端口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public interface BatchProcessingScheduler {

    /**
     * 调度批次处理。
     *
     * @param batchId 批次 ID
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    void schedule(String batchId);
}
