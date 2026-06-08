package io.github.lvdaxianer.doclens.j.adapter.domain;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * OCR 节点图片调用记录创建请求。
 *
 * @param id 调用记录 ID
 * @param batchId 批次 ID
 * @param documentId 文档 ID
 * @param pageNo 页码
 * @param modelKey OCR 模型标识
 * @param nodeId OCR 节点标识
 * @param routingMode 路由模式
 * @param status 调用状态
 * @param retryCount 重试次数
 * @param elapsedMs 耗时毫秒
 * @param errorCode 错误码
 * @param errorMessage 错误消息
 * @param startedAt 开始时间
 * @param finishedAt 完成时间
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record OcrNodeCallCreateRequest(
        String id,
        String batchId,
        String documentId,
        int pageNo,
        String modelKey,
        String nodeId,
        OcrRoutingMode routingMode,
        OcrNodeCallStatus status,
        int retryCount,
        long elapsedMs,
        Optional<String> errorCode,
        Optional<String> errorMessage,
        OffsetDateTime startedAt,
        Optional<OffsetDateTime> finishedAt
) {

    /**
     * 创建带安全默认值的调用记录创建请求。
     *
     * @param id 调用记录 ID
     * @param batchId 批次 ID
     * @param documentId 文档 ID
     * @param pageNo 页码
     * @param modelKey OCR 模型标识
     * @param nodeId OCR 节点标识
     * @param routingMode 路由模式
     * @param status 调用状态
     * @param retryCount 重试次数
     * @param elapsedMs 耗时毫秒
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @param startedAt 开始时间
     * @param finishedAt 完成时间
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public OcrNodeCallCreateRequest {
        errorCode = errorCode == null ? Optional.empty() : errorCode;
        errorMessage = errorMessage == null ? Optional.empty() : errorMessage;
        finishedAt = finishedAt == null ? Optional.empty() : finishedAt;
    }
}
