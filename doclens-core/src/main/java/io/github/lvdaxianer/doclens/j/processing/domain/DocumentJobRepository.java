package io.github.lvdaxianer.doclens.j.processing.domain;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for document jobs.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public interface DocumentJobRepository {

    /**
     * Saves a new document job.
     *
     * @param document document job
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void save(DocumentJob document);

    /**
     * Saves document jobs in batch.
     *
     * @param documents document jobs
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void saveAll(List<DocumentJob> documents);

    /**
     * Updates an existing document job.
     *
     * @param document document job
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void update(DocumentJob document);

    /**
     * Updates document jobs in batch.
     *
     * @param documents document jobs
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void updateAll(List<DocumentJob> documents);

    /**
     * Finds a document by id.
     *
     * @param documentId document id
     * @return optional document
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Optional<DocumentJob> findById(String documentId);

    /**
     * Lists documents by batch ordered by upload order.
     *
     * @param batchId batch id
     * @return ordered documents
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    List<DocumentJob> listByBatchId(String batchId);
}
