package com.doclens.processing.domain;

import java.util.Optional;

/**
 * Repository interface for OCR results.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public interface OcrResultRepository {

    /**
     * Saves an OCR result.
     *
     * @param result OCR result
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void save(OcrResult result);

    /**
     * Saves OCR results in batch.
     *
     * @param results OCR results
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void saveAll(java.util.List<OcrResult> results);

    /**
     * Finds OCR result by document id.
     *
     * @param documentId document id
     * @return optional result
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Optional<OcrResult> findByDocumentId(String documentId);
}
