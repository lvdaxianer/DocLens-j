package io.github.lvdaxianer.doclens.j.processing.domain;

import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.util.Map;
import java.util.Optional;

/**
 * Request object for creating OCR lifecycle events.
 *
 * @param batchId batch id
 * @param documentId optional document id
 * @param eventType event type
 * @param status event status
 * @param stage event stage
 * @param progress progress payload
 * @param metadata metadata payload
 * @param resultId optional result id
 * @param resultSummary result summary payload
 * @param error error payload
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record EventCreateRequest(
        String batchId,
        Optional<String> documentId,
        String eventType,
        String status,
        String stage,
        Map<String, Object> progress,
        JsonPayload metadata,
        Optional<String> resultId,
        Map<String, Object> resultSummary,
        Map<String, Object> error
) {

    /**
     * Creates an event request with safe optional and map defaults.
     *
     * @param batchId batch id
     * @param documentId optional document id
     * @param eventType event type
     * @param status event status
     * @param stage event stage
     * @param progress progress payload
     * @param metadata metadata payload
     * @param resultId optional result id
     * @param resultSummary result summary payload
     * @param error error payload
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public EventCreateRequest {
        documentId = documentId == null ? Optional.empty() : documentId;
        resultId = resultId == null ? Optional.empty() : resultId;
        progress = progress == null ? Map.of() : Map.copyOf(progress);
        metadata = metadata == null ? JsonPayload.empty() : metadata;
        resultSummary = resultSummary == null ? Map.of() : Map.copyOf(resultSummary);
        error = error == null ? Map.of() : Map.copyOf(error);
    }
}
