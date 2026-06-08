package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;

/**
 * OCR 路由执行结果。
 *
 * @param result 图片 OCR 结果
 * @param modelKey 命中的 OCR 模型标识
 * @param nodeId 命中的 OCR 节点 ID
 * @param elapsedMs 总耗时毫秒
 * @param retryCount 失败重试次数
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record OcrRouteExecutionResult(
        ImageOcrResult result,
        String modelKey,
        String nodeId,
        long elapsedMs,
        int retryCount
) {
}
