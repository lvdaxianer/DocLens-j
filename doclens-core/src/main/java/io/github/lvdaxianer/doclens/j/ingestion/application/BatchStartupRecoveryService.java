package io.github.lvdaxianer.doclens.j.ingestion.application;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 批次启动恢复服务。
 *
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
public class BatchStartupRecoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchStartupRecoveryService.class);

    private final DocumentJobRepository documentRepository;
    private final BatchProcessingScheduler batchProcessingScheduler;

    /**
     * 创建批次启动恢复服务。
     *
     * @param documentRepository 文档任务仓储
     * @param batchProcessingScheduler 批次处理调度器
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    public BatchStartupRecoveryService(
            DocumentJobRepository documentRepository,
            BatchProcessingScheduler batchProcessingScheduler
    ) {
        this.documentRepository = documentRepository;
        this.batchProcessingScheduler = batchProcessingScheduler;
    }

    /**
     * 恢复仍有排队文档的批次。
     *
     * @param limit 最大恢复批次数量
     * @return 已调度批次数量
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    public int recoverQueuedBatches(int limit) {
        List<String> batchIds = documentRepository.listQueuedBatchIds(limit);
        batchIds.forEach(batchProcessingScheduler::schedule);
        LOGGER.info("[批次恢复] 启动恢复完成, recoveredBatchCount={}", batchIds.size());
        return batchIds.size();
    }
}
