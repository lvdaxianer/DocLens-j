package io.github.lvdaxianer.doclens.j.ingestion.domain;

import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 一次上传请求对应的批次聚合根。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record Batch(
        String batchId,
        BatchStatus status,
        int totalFiles,
        int completedFiles,
        int failedFiles,
        Optional<String> currentDocumentId,
        Optional<String> currentDocumentName,
        String currentStage,
        JsonPayload metadata,
        Optional<String> callbackUrl,
        Optional<String> idempotencyKey,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /**
     * 创建带安全可选默认值的批次。
     *
     * @param batchId 批次 ID
     * @param status 批次状态
     * @param totalFiles 文件总数
     * @param completedFiles 已完成文件数
     * @param failedFiles 失败文件数
     * @param currentDocumentId 当前文档 ID
     * @param currentDocumentName 当前文档名称
     * @param currentStage 当前处理阶段
     * @param metadata 元数据载荷
     * @param callbackUrl 回调 URL
     * @param idempotencyKey 幂等键
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public Batch {
        currentDocumentId = currentDocumentId == null ? Optional.empty() : currentDocumentId;
        currentDocumentName = currentDocumentName == null ? Optional.empty() : currentDocumentName;
        metadata = metadata == null ? JsonPayload.empty() : metadata;
        callbackUrl = callbackUrl == null ? Optional.empty() : callbackUrl;
        idempotencyKey = idempotencyKey == null ? Optional.empty() : idempotencyKey;
    }

    /**
     * 创建排队中的批次。
     *
     * @param batchId 批次 ID
     * @param totalFiles 文件总数
     * @param metadata 元数据载荷
     * @param callbackUrl 回调 URL
     * @param idempotencyKey 幂等键
     * @param now 当前时间
     * @return 排队中的批次
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public static Batch create(
            String batchId,
            int totalFiles,
            JsonPayload metadata,
            Optional<String> callbackUrl,
            Optional<String> idempotencyKey,
            OffsetDateTime now
    ) {
        if (totalFiles > 0) {
            return new Batch(batchId, BatchStatus.QUEUED, totalFiles, 0, 0, Optional.empty(), Optional.empty(), "queued",
                    metadata, callbackUrl, idempotencyKey, now, now);
        } else {
            throw new IllegalArgumentException("files is required");
        }
    }
}
