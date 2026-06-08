package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * OCR 节点调用记录请求。
 *
 * @param imageRequest 图片 OCR 请求
 * @param policy OCR 路由策略
 * @param node OCR 节点
 * @param status 调用状态
 * @param retryCount 重试次数
 * @param elapsedMs 耗时毫秒
 * @param errorCode 错误码
 * @param errorMessage 错误消息
 * @param startedAt 开始时间
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
record OcrNodeCallRecordRequest(
        ImageOcrRequest imageRequest,
        OcrRoutePolicy policy,
        OcrRuntimeNodeView node,
        OcrNodeCallStatus status,
        int retryCount,
        long elapsedMs,
        Optional<String> errorCode,
        Optional<String> errorMessage,
        OffsetDateTime startedAt
) {
}
