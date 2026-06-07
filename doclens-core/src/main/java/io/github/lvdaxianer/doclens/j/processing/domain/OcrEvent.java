package io.github.lvdaxianer.doclens.j.processing.domain;

import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * OCR 生命周期事件。
 *
 * @param eventId 事件 ID
 * @param eventType 事件类型
 * @param batchId 批次 ID
 * @param documentId 可选文档 ID
 * @param status 关联状态
 * @param stage 关联阶段
 * @param progress 进度载荷
 * @param metadata 元数据载荷
 * @param resultId 可选结果 ID
 * @param resultSummary 可选结果摘要
 * @param error 可选错误载荷
 * @param occurredAt 发生时间
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record OcrEvent(
        String eventId,
        String eventType,
        String batchId,
        Optional<String> documentId,
        String status,
        String stage,
        Map<String, Object> progress,
        JsonPayload metadata,
        Optional<String> resultId,
        Map<String, Object> resultSummary,
        Map<String, Object> error,
        OffsetDateTime occurredAt
) {
    /**
     * 创建带安全默认值的 OCR 事件。
     *
     * @param eventId 事件 ID
     * @param eventType 事件类型
     * @param batchId 批次 ID
     * @param documentId 可选文档 ID
     * @param status 事件状态
     * @param stage 事件阶段
     * @param progress 进度载荷
     * @param metadata 元数据载荷
     * @param resultId 可选结果 ID
     * @param resultSummary 结果摘要载荷
     * @param error 错误载荷
     * @param occurredAt 发生时间
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public OcrEvent {
        documentId = documentId == null ? Optional.empty() : documentId;
        resultId = resultId == null ? Optional.empty() : resultId;
        progress = progress == null ? Map.of() : Map.copyOf(progress);
        metadata = metadata == null ? JsonPayload.empty() : metadata;
        resultSummary = resultSummary == null ? Map.of() : Map.copyOf(resultSummary);
        error = error == null ? Map.of() : Map.copyOf(error);
    }
}
