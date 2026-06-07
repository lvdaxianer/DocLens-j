package io.github.lvdaxianer.doclens.j.processing.domain;

import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Request object for creating document jobs.
 *
 * @param documentId document id
 * @param batchId batch id
 * @param fileName original file name
 * @param fileType detected file type
 * @param fileSize file size
 * @param pageCount detected page count
 * @param storageUri upload storage URI
 * @param adapterName adapter key
 * @param pdfMode PDF processing mode
 * @param metadata metadata payload
 * @param sortOrder upload order
 * @param now current time
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record DocumentJobCreateRequest(
        String documentId,
        String batchId,
        String fileName,
        DocumentType fileType,
        long fileSize,
        int pageCount,
        String storageUri,
        String adapterName,
        Optional<PdfMode> pdfMode,
        JsonPayload metadata,
        int sortOrder,
        OffsetDateTime now
) {
    /**
     * Creates a document job request with safe defaults.
     *
     * @param documentId document id
     * @param batchId batch id
     * @param fileName file name
     * @param fileType file type
     * @param fileSize file size
     * @param pageCount page count
     * @param storageUri storage URI
     * @param adapterName adapter key
     * @param pdfMode optional PDF mode
     * @param metadata metadata payload
     * @param sortOrder upload order
     * @param now current time
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public DocumentJobCreateRequest {
        pdfMode = pdfMode == null ? Optional.empty() : pdfMode;
        metadata = metadata == null ? JsonPayload.empty() : metadata;
    }
}
