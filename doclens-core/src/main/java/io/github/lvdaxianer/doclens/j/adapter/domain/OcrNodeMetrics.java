package io.github.lvdaxianer.doclens.j.adapter.domain;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * OCR 节点运行指标。
 *
 * @param inflightImages 正在解析图片数
 * @param queuedImages 排队图片数
 * @param processedImagesToday 今日处理图片数
 * @param successImages 成功图片数
 * @param failedImages 失败图片数
 * @param avgLatencyMs 平均耗时
 * @param p95LatencyMs P95 耗时
 * @param lastRequestAt 最近请求时间
 * @param lastError 最近错误
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record OcrNodeMetrics(
        int inflightImages,
        int queuedImages,
        long processedImagesToday,
        long successImages,
        long failedImages,
        long avgLatencyMs,
        long p95LatencyMs,
        Optional<OffsetDateTime> lastRequestAt,
        Optional<String> lastError
) {

    /**
     * 创建带安全默认值的节点指标。
     *
     * @param inflightImages 正在解析图片数
     * @param queuedImages 排队图片数
     * @param processedImagesToday 今日处理图片数
     * @param successImages 成功图片数
     * @param failedImages 失败图片数
     * @param avgLatencyMs 平均耗时
     * @param p95LatencyMs P95 耗时
     * @param lastRequestAt 最近请求时间
     * @param lastError 最近错误
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public OcrNodeMetrics {
        lastRequestAt = lastRequestAt == null ? Optional.empty() : lastRequestAt;
        lastError = lastError == null ? Optional.empty() : lastError;
    }
}
