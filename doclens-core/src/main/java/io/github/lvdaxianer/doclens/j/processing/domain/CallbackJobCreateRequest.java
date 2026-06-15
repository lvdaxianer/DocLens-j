package io.github.lvdaxianer.doclens.j.processing.domain;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 回调任务创建请求。
 *
 * @param callbackJobId 回调任务 ID
 * @param eventId 事件 ID
 * @param batchId 批次 ID
 * @param documentId 可选文档 ID
 * @param callbackUrl 回调地址
 * @param payload 回调载荷
 * @param now 创建时间
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
public record CallbackJobCreateRequest(
        String callbackJobId,
        String eventId,
        String batchId,
        String documentId,
        String callbackUrl,
        Map<String, Object> payload,
        OffsetDateTime now
) {
    /**
     * 创建带安全默认值的回调任务创建请求。
     *
     * @param callbackJobId 回调任务 ID
     * @param eventId 事件 ID
     * @param batchId 批次 ID
     * @param documentId 可选文档 ID
     * @param callbackUrl 回调地址
     * @param payload 回调载荷
     * @param now 创建时间
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public CallbackJobCreateRequest {
        documentId = documentId == null || documentId.isBlank() ? null : documentId;
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
