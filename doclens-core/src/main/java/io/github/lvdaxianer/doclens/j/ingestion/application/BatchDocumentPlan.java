package io.github.lvdaxianer.doclens.j.ingestion.application;

import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.time.OffsetDateTime;

/**
 * Context for creating document jobs within a batch.
 *
 * @param command create batch command
 * @param batchId batch id
 * @param metadata metadata payload
 * @param now creation time
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record BatchDocumentPlan(
        CreateBatchCommand command,
        String batchId,
        JsonPayload metadata,
        OffsetDateTime now
) {
}
