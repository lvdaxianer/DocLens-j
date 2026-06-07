package io.github.lvdaxianer.doclens.j.api;

import java.util.List;
import java.util.Map;

/**
 * Embedded SDK request for creating an OCR batch.
 *
 * @param files upload files
 * @param metadata business metadata
 * @param callbackUrl optional callback URL
 * @param idempotencyKey optional idempotency key
 * @param adapterOverride optional OCR adapter key
 * @param pdfMode optional PDF processing mode
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record CreateBatchRequest(
        List<DocumentInput> files,
        Map<String, Object> metadata,
        String callbackUrl,
        String idempotencyKey,
        String adapterOverride,
        String pdfMode
) {
}
