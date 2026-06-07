package io.github.lvdaxianer.doclens.j.ingestion.domain;

import java.util.Optional;

/**
 * Repository interface for batch aggregate.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public interface BatchRepository {

    /**
     * Saves a batch.
     *
     * @param batch batch aggregate
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void save(Batch batch);

    /**
     * Finds a batch by id.
     *
     * @param batchId batch id
     * @return optional batch
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Optional<Batch> findById(String batchId);

    /**
     * Finds a batch by idempotency key.
     *
     * @param idempotencyKey idempotency key
     * @return optional batch
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Optional<Batch> findByIdempotencyKey(String idempotencyKey);

    /**
     * Updates batch processing summary.
     *
     * @param batchId batch id
     * @param completedFiles completed file count
     * @param failedFiles failed file count
     * @param status final status
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void updateSummary(String batchId, int completedFiles, int failedFiles, BatchStatus status);
}
