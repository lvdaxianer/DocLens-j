package io.github.lvdaxianer.doclens.j.processing.domain;

import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.util.Map;
import java.util.Optional;

/**
 * 创建 OCR 生命周期事件的请求对象。
 *
 * @param batchId 批次 ID
 * @param documentId 可选文档 ID
 * @param eventType 事件类型
 * @param status 事件状态
 * @param stage 事件阶段
 * @param progress 进度载荷
 * @param metadata 元数据载荷
 * @param resultId 可选结果 ID
 * @param resultSummary 结果摘要载荷
 * @param error 错误载荷
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record EventCreateRequest(
        String batchId,
        Optional<String> documentId,
        String eventType,
        String status,
        String stage,
        Map<String, Object> progress,
        JsonPayload metadata,
        Optional<String> resultId,
        Map<String, Object> resultSummary,
        Map<String, Object> error
) {

    /**
     * 创建带安全可选值和 Map 默认值的事件请求。
     *
     * @param batchId 批次 ID
     * @param documentId 可选文档 ID
     * @param eventType 事件类型
     * @param status 事件状态
     * @param stage 事件阶段
     * @param progress 进度载荷
     * @param metadata 元数据载荷
     * @param resultId 可选结果 ID
     * @param resultSummary 结果摘要载荷
     * @param error 错误载荷
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public EventCreateRequest {
        documentId = documentId == null ? Optional.empty() : documentId;
        resultId = resultId == null ? Optional.empty() : resultId;
        progress = progress == null ? Map.of() : Map.copyOf(progress);
        metadata = metadata == null ? JsonPayload.empty() : metadata;
        resultSummary = resultSummary == null ? Map.of() : Map.copyOf(resultSummary);
        error = error == null ? Map.of() : Map.copyOf(error);
    }
}
