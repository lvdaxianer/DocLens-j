package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 文档删除测试使用的内存批次仓储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class DeleteUseCaseBatchRepository implements BatchRepository {

    /** 测试集合初始容量。 */
    private static final int TEST_CAPACITY = 8;
    /** 按批次 ID 保存的批次。 */
    private final Map<String, Batch> batches = new HashMap<>(TEST_CAPACITY);

    /**
     * 保存批次。
     *
     * @param batch 批次
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void save(Batch batch) {
        batches.put(batch.batchId(), batch);
    }

    /**
     * 根据批次 ID 查询批次。
     *
     * @param batchId 批次 ID
     * @return 批次
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public Optional<Batch> findById(String batchId) {
        return Optional.ofNullable(batches.get(batchId));
    }

    /**
     * 根据幂等键查询批次。
     *
     * @param idempotencyKey 幂等键
     * @return 批次
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public Optional<Batch> findByIdempotencyKey(String idempotencyKey) {
        return Optional.empty();
    }

    /**
     * 查询最近批次。
     *
     * @param limit 查询上限
     * @return 批次列表
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public List<Batch> listRecent(int limit) {
        return batches.values().stream().limit(limit).toList();
    }

    /**
     * 更新批次摘要。
     *
     * @param batchId 批次 ID
     * @param completedFiles 已完成文件数
     * @param failedFiles 失败文件数
     * @param status 批次状态
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void updateSummary(String batchId, int completedFiles, int failedFiles, BatchStatus status) {
        Batch batch = batches.get(batchId);
        if (batch == null) {
            return;
        } else {
            batches.put(batchId, refreshedBatch(batch, completedFiles, failedFiles, status));
        }
    }

    /**
     * 根据批次 ID 删除批次。
     *
     * @param batchId 批次 ID
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void deleteById(String batchId) {
        batches.remove(batchId);
    }

    /**
     * 刷新批次摘要。
     *
     * @param batch 原批次
     * @param completedFiles 已完成文件数
     * @param failedFiles 失败文件数
     * @param status 批次状态
     * @return 刷新后的批次
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private Batch refreshedBatch(Batch batch, int completedFiles, int failedFiles, BatchStatus status) {
        int totalFiles = Math.max(0, batch.totalFiles() - 1);
        return new Batch(batch.batchId(), status, totalFiles, completedFiles, failedFiles,
                batch.currentDocumentId(), batch.currentDocumentName(), batch.currentStage(), batch.metadata(),
                batch.callbackUrl(), batch.idempotencyKey(), batch.createdAt(), OffsetDateTime.now());
    }
}
