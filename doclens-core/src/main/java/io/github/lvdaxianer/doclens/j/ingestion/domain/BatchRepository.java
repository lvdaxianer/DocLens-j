package io.github.lvdaxianer.doclens.j.ingestion.domain;

import java.util.List;
import java.util.Optional;

/**
 * 批次聚合仓储接口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public interface BatchRepository {

    /**
     * 保存批次。
     *
     * @param batch 批次聚合
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void save(Batch batch);

    /**
     * 根据 ID 查找批次。
     *
     * @param batchId 批次 ID
     * @return 可选批次
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Optional<Batch> findById(String batchId);

    /**
     * 根据幂等键查找批次。
     *
     * @param idempotencyKey 幂等键
     * @return 可选批次
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Optional<Batch> findByIdempotencyKey(String idempotencyKey);

    /**
     * 按更新时间倒序列出最近批次。
     *
     * @param limit 最大返回数量
     * @return 最近批次集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    List<Batch> listRecent(int limit);

    /**
     * 根据 ID 删除批次。
     *
     * @param batchId 批次 ID
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    default void deleteById(String batchId) {
        throw new UnsupportedOperationException("batch delete is not supported");
    }

    /**
     * 更新批次处理摘要。
     *
     * @param batchId 批次 ID
     * @param completedFiles 已完成文件数
     * @param failedFiles 失败文件数
     * @param status 最终状态
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void updateSummary(String batchId, int completedFiles, int failedFiles, BatchStatus status);

    /**
     * 更新批次处理摘要，并显式同步总文件数。
     *
     * @param batchId 批次 ID
     * @param totalFiles 总文件数
     * @param completedFiles 已完成文件数
     * @param failedFiles 失败文件数
     * @param status 最终状态
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    default void updateSummary(
            String batchId,
            int totalFiles,
            int completedFiles,
            int failedFiles,
            BatchStatus status
    ) {
        updateSummary(batchId, completedFiles, failedFiles, status);
    }
}
