package io.github.lvdaxianer.doclens.j.api;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Public DocLens lifecycle event for callback and embedded listeners.
 *
 * @param eventId event identifier
 * @param eventType event type
 * @param batchId batch identifier
 * @param documentId optional document identifier
 * @param status public status
 * @param stage public stage
 * @param progress progress payload
 * @param metadata business metadata
 * @param occurredAt event time
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record DocLensEvent(
        String eventId,
        String eventType,
        String batchId,
        Optional<String> documentId,
        String status,
        String stage,
        Map<String, Object> progress,
        Map<String, Object> metadata,
        OffsetDateTime occurredAt
) {
}
