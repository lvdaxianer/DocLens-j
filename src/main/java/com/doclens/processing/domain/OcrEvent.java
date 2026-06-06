package com.doclens.processing.domain;

import com.doclens.shared.domain.JsonPayload;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * OCR lifecycle event.
 *
 * @param eventId event id
 * @param eventType event type
 * @param batchId batch id
 * @param documentId optional document id
 * @param status related status
 * @param stage related stage
 * @param progress progress payload
 * @param metadata metadata payload
 * @param resultId optional result id
 * @param resultSummary optional result summary
 * @param error optional error payload
 * @param occurredAt occurrence time
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record OcrEvent(
        String eventId,
        String eventType,
        String batchId,
        Optional<String> documentId,
        String status,
        String stage,
        Map<String, Object> progress,
        JsonPayload metadata,
        Optional<String> resultId,
        Map<String, Object> resultSummary,
        Map<String, Object> error,
        OffsetDateTime occurredAt
) {
    /**
     * Creates an OCR event with safe defaults.
     *
     * @param eventId event id
     * @param eventType event type
     * @param batchId batch id
     * @param documentId optional document id
     * @param status event status
     * @param stage event stage
     * @param progress progress payload
     * @param metadata metadata payload
     * @param resultId optional result id
     * @param resultSummary result summary payload
     * @param error error payload
     * @param occurredAt occurrence time
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public OcrEvent {
        documentId = documentId == null ? Optional.empty() : documentId;
        resultId = resultId == null ? Optional.empty() : resultId;
        progress = progress == null ? Map.of() : Map.copyOf(progress);
        metadata = metadata == null ? JsonPayload.empty() : metadata;
        resultSummary = resultSummary == null ? Map.of() : Map.copyOf(resultSummary);
        error = error == null ? Map.of() : Map.copyOf(error);
    }
}
