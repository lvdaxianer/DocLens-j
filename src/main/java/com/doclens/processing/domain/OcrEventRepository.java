package com.doclens.processing.domain;

import java.util.List;

/**
 * Repository interface for OCR lifecycle events.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public interface OcrEventRepository {

    /**
     * Saves an event.
     *
     * @param event OCR event
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void save(OcrEvent event);

    /**
     * Saves events in batch.
     *
     * @param events OCR events
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void saveAll(List<OcrEvent> events);

    /**
     * Lists events by batch id.
     *
     * @param batchId batch id
     * @return ordered events
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    List<OcrEvent> listByBatchId(String batchId);
}
