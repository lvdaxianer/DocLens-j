package com.doclens.ingestion.application;

import java.util.List;
import java.util.Map;

/**
 * Create batch command.
 *
 * @param files uploaded files
 * @param metadata metadata payload
 * @param callbackUrl callback URL
 * @param idempotencyKey idempotency key
 * @param adapterOverride adapter override
 * @param pdfMode PDF mode
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record CreateBatchCommand(
        List<UploadFileCommand> files,
        Map<String, Object> metadata,
        String callbackUrl,
        String idempotencyKey,
        String adapterOverride,
        String pdfMode
) {
}
