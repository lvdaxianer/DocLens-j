package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeMetrics;
import java.util.Optional;

/**
 * OCR 节点响应 DTO。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record OcrNodeResponse(
        String id,
        @JsonProperty("model_key") String modelKey,
        @JsonProperty("deployment_type") String deploymentType,
        String name,
        String host,
        int port,
        @JsonProperty("channel_key") String channelKey,
        @JsonProperty("provider_model") String providerModel,
        @JsonProperty("credential_configured") boolean credentialConfigured,
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
        return from(node, defaultMetrics());
    }

    /**
     * 从领域节点和聚合指标创建响应。
     *
     * @param node OCR 节点
     * @param metrics OCR 节点聚合指标
     * @return OCR 节点响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public static OcrNodeResponse from(OcrNode node, OcrNodeMetrics metrics) {
        return new OcrNodeResponse(node.id(), node.modelKey(), node.deploymentType().name(), node.name(), node.host(),
                node.port(), node.channelKey().orElse(""), node.providerModel().orElse(""),
                node.credentialConfigured(), node.enabled(), node.participateGlobal(), node.weight(),
                node.maxConcurrency(), node.status().name(), metrics.inflightImages(), metrics.queuedImages(),
                metrics.processedImagesToday(), metrics.successImages(), metrics.failedImages(), metrics.avgLatencyMs(),
                metrics.p95LatencyMs(),
                node.lastHealthAt().map(Object::toString).orElse(""), node.lastError().orElse(""));
    }

    /**
     * 创建默认空指标。
     *
     * @return 默认空指标
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static OcrNodeMetrics defaultMetrics() {
        return new OcrNodeMetrics(0, 0, 0L, 0L, 0L, 0L, 0L, Optional.empty(), Optional.empty());
    }
}
