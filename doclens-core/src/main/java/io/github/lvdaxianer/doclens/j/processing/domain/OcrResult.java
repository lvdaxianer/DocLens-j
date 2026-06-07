package io.github.lvdaxianer.doclens.j.processing.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * OCR structured result aggregate.
 *
 * @param resultId result id
 * @param documentId document id
 * @param rawVendorOutput raw vendor output
 * @param structuredDocument normalized structured document
 * @param pageText page text records
 * @param layoutBlocks layout blocks
 * @param tables table records
 * @param images image records
 * @param confidence OCR confidence
 * @param warnings normalization warnings
 * @param createdAt creation time
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record OcrResult(
        String resultId,
        String documentId,
        Map<String, Object> rawVendorOutput,
        Map<String, Object> structuredDocument,
        List<Map<String, Object>> pageText,
        List<Map<String, Object>> layoutBlocks,
        List<Map<String, Object>> tables,
        List<Map<String, Object>> images,
        double confidence,
        List<String> warnings,
        OffsetDateTime createdAt
) {
}
