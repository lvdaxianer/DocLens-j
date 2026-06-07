package io.github.lvdaxianer.doclens.j.api;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * 面向回调和嵌入式监听器的公开 DocLens 生命周期事件。
 *
 * @param eventId 事件标识
 * @param eventType 事件类型
 * @param batchId 批次标识
 * @param documentId 可选文档标识
 * @param status 公开状态
 * @param stage 公开阶段
 * @param progress 进度载荷
 * @param metadata 业务元数据
 * @param occurredAt 事件时间
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record DocLensEvent(
        String eventId,
        String eventType,
        String batchId,
        Optional<String> documentId,
        String status,
        String stage,
        Map<String, Object> progress,
        Map<String, Object> metadata,
        OffsetDateTime occurredAt
) {
}
