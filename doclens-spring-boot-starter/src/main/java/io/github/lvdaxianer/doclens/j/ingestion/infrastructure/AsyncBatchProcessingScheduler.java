package io.github.lvdaxianer.doclens.j.ingestion.infrastructure;

import io.github.lvdaxianer.doclens.j.ingestion.application.BatchProcessingScheduler;
import io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCase;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于专用线程池的批次处理调度器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class AsyncBatchProcessingScheduler implements BatchProcessingScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncBatchProcessingScheduler.class);

    private final BatchProcessingUseCase batchProcessingUseCase;
    private final ExecutorService executorService;

    /**
     * 创建异步批次处理调度器。
     *
     * @param batchProcessingUseCase 批次处理用例
     * @param executorService 批次处理线程池
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public AsyncBatchProcessingScheduler(
            BatchProcessingUseCase batchProcessingUseCase,
            ExecutorService executorService
    ) {
        this.batchProcessingUseCase = batchProcessingUseCase;
        this.executorService = executorService;
    }

    /**
     * 提交批次后台处理任务。
     *
     * @param batchId 批次 ID
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Override
    public void schedule(String batchId) {
        executorService.submit(() -> processBatch(batchId));
    }

    /**
     * 执行批次处理并记录后台异常。
     *
     * @param batchId 批次 ID
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private void processBatch(String batchId) {
        try {
            batchProcessingUseCase.processBatch(batchId);
        } catch (RuntimeException ex) {
            LOGGER.warn("[OCR处理] 后台批次处理失败 batchId={}, error={}", batchId, ex.getMessage(), ex);
        }
    }
}
