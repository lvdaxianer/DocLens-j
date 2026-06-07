package io.github.lvdaxianer.doclens.j.api;

import java.util.Map;

/**
 * Stable embedded entrypoint for DocLens OCR capabilities.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public interface DocLensEngine {

    /**
     * Creates an OCR batch from embedded SDK input.
     *
     * @param request create batch request
     * @return batch creation response
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Map<String, Object> createBatch(CreateBatchRequest request);

    /**
     * Gets batch status and progress.
     *
     * @param batchId batch identifier
     * @return batch view
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Map<String, Object> getBatch(String batchId);

    /**
     * Gets one document status and progress.
     *
     * @param documentId document identifier
     * @return document view
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Map<String, Object> getDocument(String documentId);

    /**
     * Gets OCR result for one document.
     *
     * @param documentId document identifier
     * @return OCR result view
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Map<String, Object> getDocumentResult(String documentId);

    /**
     * Gets event timeline for one batch.
     *
     * @param batchId batch identifier
     * @return event timeline
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Map<String, Object> getEvents(String batchId);
}
