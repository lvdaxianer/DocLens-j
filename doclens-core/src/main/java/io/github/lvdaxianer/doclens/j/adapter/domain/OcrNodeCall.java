package io.github.lvdaxianer.doclens.j.adapter.domain;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 图片级 OCR 节点调用记录。
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
public record OcrNodeCall(
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
     * 创建带安全默认值的调用记录。
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
    public OcrNodeCall {
        id = requiredText(id, "ocr node call id is required");
        batchId = requiredText(batchId, "batch id is required");
        documentId = requiredText(documentId, "document id is required");
        modelKey = requiredText(modelKey, "ocr model key is required");
        nodeId = requiredText(nodeId, "ocr node id is required");
        routingMode = requireRoutingMode(routingMode);
        status = requireStatus(status);
        errorCode = errorCode == null ? Optional.empty() : errorCode;
        errorMessage = errorMessage == null ? Optional.empty() : errorMessage;
        finishedAt = finishedAt == null ? Optional.empty() : finishedAt;
    }

    /**
     * 创建图片级 OCR 调用记录。
     *
     * @param request 调用记录创建请求
     * @return OCR 调用记录
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public static OcrNodeCall create(OcrNodeCallCreateRequest request) {
        return new OcrNodeCall(request.id(), request.batchId(), request.documentId(), request.pageNo(),
                request.modelKey(), request.nodeId(), request.routingMode(), request.status(), request.retryCount(),
                request.elapsedMs(), request.errorCode(), request.errorMessage(), request.startedAt(),
                request.finishedAt());
    }

    /**
     * 校验必填文本。
     *
     * @param value 文本值
     * @param message 校验失败消息
     * @return 标准化后的文本
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        } else {
            return value.trim();
        }
    }

    /**
     * 校验路由模式。
     *
     * @param routingMode 路由模式
     * @return 非空路由模式
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static OcrRoutingMode requireRoutingMode(OcrRoutingMode routingMode) {
        if (routingMode != null) {
            return routingMode;
        } else {
            throw new IllegalArgumentException("ocr routing mode is required");
        }
    }

    /**
     * 校验调用状态。
     *
     * @param status 调用状态
     * @return 非空调用状态
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static OcrNodeCallStatus requireStatus(OcrNodeCallStatus status) {
        if (status != null) {
            return status;
        } else {
            throw new IllegalArgumentException("ocr node call status is required");
        }
    }
}
