package io.github.lvdaxianer.doclens.j.ingestion.application;

import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.time.OffsetDateTime;

/**
 * 批次内创建文档任务所需的上下文。
 *
 * @param command 创建批次命令
 * @param batchId 批次 ID
 * @param metadata 元数据载荷
 * @param now 创建时间
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record BatchDocumentPlan(
        CreateBatchCommand command,
        String batchId,
        JsonPayload metadata,
        OffsetDateTime now
) {
}
