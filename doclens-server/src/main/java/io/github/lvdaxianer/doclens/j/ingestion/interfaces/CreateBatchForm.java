package io.github.lvdaxianer.doclens.j.ingestion.interfaces;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * Multipart form object for creating OCR batches.
 *
 * @param files uploaded files
 * @param metadata metadata JSON
 * @param callbackUrl callback URL
 * @param idempotencyKey idempotency key
 * @param adapterOverride adapter override
 * @param pdfMode PDF mode
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record CreateBatchForm(
        List<MultipartFile> files,
        String metadata,
        String callbackUrl,
        String idempotencyKey,
        String adapterOverride,
        String pdfMode
) {
}
