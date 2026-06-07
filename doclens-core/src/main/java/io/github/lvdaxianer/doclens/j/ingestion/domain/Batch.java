package io.github.lvdaxianer.doclens.j.ingestion.domain;

import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Batch aggregate root for one upload request.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record Batch(
        String batchId,
        BatchStatus status,
        int totalFiles,
        int completedFiles,
        int failedFiles,
        Optional<String> currentDocumentId,
        Optional<String> currentDocumentName,
        String currentStage,
        JsonPayload metadata,
        Optional<String> callbackUrl,
        Optional<String> idempotencyKey,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /**
     * Creates a batch with safe optional defaults.
     *
     * @param batchId batch id
     * @param status batch status
     * @param totalFiles total file count
     * @param completedFiles completed file count
     * @param failedFiles failed file count
     * @param currentDocumentId current document id
     * @param currentDocumentName current document name
     * @param currentStage current processing stage
     * @param metadata metadata payload
     * @param callbackUrl callback URL
     * @param idempotencyKey idempotency key
     * @param createdAt creation time
     * @param updatedAt update time
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public Batch {
        currentDocumentId = currentDocumentId == null ? Optional.empty() : currentDocumentId;
        currentDocumentName = currentDocumentName == null ? Optional.empty() : currentDocumentName;
        metadata = metadata == null ? JsonPayload.empty() : metadata;
        callbackUrl = callbackUrl == null ? Optional.empty() : callbackUrl;
        idempotencyKey = idempotencyKey == null ? Optional.empty() : idempotencyKey;
    }

    /**
     * Creates a queued batch.
     *
     * @param batchId batch id
     * @param totalFiles total file count
     * @param metadata metadata payload
     * @param callbackUrl callback URL
     * @param idempotencyKey idempotency key
     * @param now current time
     * @return queued batch
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public static Batch create(
            String batchId,
            int totalFiles,
            JsonPayload metadata,
            Optional<String> callbackUrl,
            Optional<String> idempotencyKey,
            OffsetDateTime now
    ) {
        if (totalFiles > 0) {
            return new Batch(batchId, BatchStatus.QUEUED, totalFiles, 0, 0, Optional.empty(), Optional.empty(), "queued",
                    metadata, callbackUrl, idempotencyKey, now, now);
        } else {
            throw new IllegalArgumentException("files is required");
        }
    }
}
