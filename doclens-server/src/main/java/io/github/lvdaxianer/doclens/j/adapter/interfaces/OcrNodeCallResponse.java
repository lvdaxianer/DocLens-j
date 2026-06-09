package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCall;

/**
 * OCR 节点最近调用响应 DTO。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record OcrNodeCallResponse(
        String id,
        @JsonProperty("document_id") String documentId,
        @JsonProperty("image_index") int imageIndex,
        String status,
        @JsonProperty("retry_count") int retryCount,
        @JsonProperty("duration_ms") long durationMs,
        @JsonProperty("error_message") String errorMessage,
        @JsonProperty("started_at") String startedAt,
        @JsonProperty("finished_at") String finishedAt
) {

    /**
     * 从领域调用记录创建响应。
     *
     * @param call OCR 节点调用记录
     * @return 调用响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public static OcrNodeCallResponse from(OcrNodeCall call) {
        return new OcrNodeCallResponse(call.id(), call.documentId(), call.pageNo(), call.status().name(),
                call.retryCount(), call.elapsedMs(), call.errorMessage().orElse(""), call.startedAt().toString(),
                call.finishedAt().map(Object::toString).orElse(""));
    }
}
