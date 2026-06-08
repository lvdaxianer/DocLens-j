package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;

/**
 * OCR 节点响应 DTO。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record OcrNodeResponse(
        String id,
        @JsonProperty("model_key") String modelKey,
        String name,
        String host,
        int port,
        boolean enabled,
        @JsonProperty("participate_global") boolean participateGlobal,
        int weight,
        @JsonProperty("max_concurrency") int maxConcurrency,
        String status,
        @JsonProperty("inflight_images") int inflightImages,
        @JsonProperty("queued_images") int queuedImages,
        @JsonProperty("processed_images_today") long processedImagesToday,
        @JsonProperty("success_images") long successImages,
        @JsonProperty("failed_images") long failedImages,
        @JsonProperty("avg_latency_ms") long avgLatencyMs,
        @JsonProperty("p95_latency_ms") long p95LatencyMs,
        @JsonProperty("last_health_at") String lastHealthAt,
        @JsonProperty("last_error") String lastError
) {

    /**
     * 从领域节点创建响应。
     *
     * @param node OCR 节点
     * @return OCR 节点响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public static OcrNodeResponse from(OcrNode node) {
        return new OcrNodeResponse(node.id(), node.modelKey(), node.name(), node.host(), node.port(),
                node.enabled(), node.participateGlobal(), node.weight(), node.maxConcurrency(), node.status().name(),
                0, 0, node.successCount() + node.failureCount(), node.successCount(), node.failureCount(),
                node.avgLatencyMs(), node.p95LatencyMs(), node.lastHealthAt().map(Object::toString).orElse(""),
                node.lastError().orElse(""));
    }
}
