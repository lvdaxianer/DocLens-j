package io.github.lvdaxianer.doclens.j.ingestion.application;

import io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCase;

/**
 * 同步执行的批次处理调度器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class InlineBatchProcessingScheduler implements BatchProcessingScheduler {

    private final BatchProcessingUseCase batchProcessingUseCase;

    /**
     * 创建同步调度器。
     *
     * @param batchProcessingUseCase 批次处理用例
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public InlineBatchProcessingScheduler(BatchProcessingUseCase batchProcessingUseCase) {
        this.batchProcessingUseCase = batchProcessingUseCase;
    }

    @Override
    public void schedule(String batchId) {
        batchProcessingUseCase.processBatch(batchId);
    }
}
