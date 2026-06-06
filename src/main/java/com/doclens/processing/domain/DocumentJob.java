package com.doclens.processing.domain;

import com.doclens.shared.domain.JsonPayload;
import com.doclens.shared.domain.DocLensConstants;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Document job aggregate root.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record DocumentJob(
        String documentId,
        String batchId,
        String fileName,
        DocumentType fileType,
        long fileSize,
        int pageCount,
        String storageUri,
        DocumentStatus status,
        ProcessingStage stage,
        int progressPercent,
        int currentPage,
        int totalPages,
        String adapterName,
        Optional<PdfMode> pdfMode,
        JsonPayload metadata,
        Optional<String> resultId,
        Optional<String> errorCode,
        Optional<String> errorMessage,
        int sortOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /**
     * Creates a document job with safe optional defaults.
     *
     * @param documentId document id
     * @param batchId batch id
     * @param fileName file name
     * @param fileType file type
     * @param fileSize file size
     * @param pageCount page count
     * @param storageUri storage URI
     * @param status document status
     * @param stage processing stage
     * @param progressPercent progress percent
     * @param currentPage current page
     * @param totalPages total pages
     * @param adapterName adapter key
     * @param pdfMode optional PDF mode
     * @param metadata metadata payload
     * @param resultId optional result id
     * @param errorCode optional error code
     * @param errorMessage optional error message
     * @param sortOrder upload order
     * @param createdAt creation time
     * @param updatedAt update time
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public DocumentJob {
        pdfMode = pdfMode == null ? Optional.empty() : pdfMode;
        metadata = metadata == null ? JsonPayload.empty() : metadata;
        resultId = resultId == null ? Optional.empty() : resultId;
        errorCode = errorCode == null ? Optional.empty() : errorCode;
        errorMessage = errorMessage == null ? Optional.empty() : errorMessage;
    }

    /**
     * Creates a queued document job.
     *
     * @param request document creation request
     * @return queued document job
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public static DocumentJob create(DocumentJobCreateRequest request) {
        return new DocumentJob(
                request.documentId(), request.batchId(), request.fileName(), request.fileType(), request.fileSize(),
                request.pageCount(), request.storageUri(), DocumentStatus.QUEUED, ProcessingStage.QUEUED, 0, 0,
                request.pageCount(), request.adapterName(), request.pdfMode(), request.metadata(), Optional.empty(), Optional.empty(), Optional.empty(),
                request.sortOrder(), request.now(), request.now()
        );
    }

    /**
     * Marks the document as processing.
     *
     * @param now current time
     * @return updated document
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public DocumentJob startProcessing(OffsetDateTime now) {
        return withState(DocumentStatus.PROCESSING, ProcessingStage.OCR_PROCESSING, DocLensConstants.START_PROGRESS_PERCENT, 0, totalPages, resultId,
                errorCode, errorMessage, now);
    }

    /**
     * Updates document page progress.
     *
     * @param currentPage current completed page
     * @param totalPages total pages
     * @param now current time
     * @return updated document
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public DocumentJob markPageCompleted(int currentPage, int totalPages, OffsetDateTime now) {
        int percent = Math.min(
                DocLensConstants.MAX_PROCESSING_PROGRESS_PERCENT,
                Math.max(DocLensConstants.START_PROGRESS_PERCENT, (int) Math.round(currentPage * 90.0 / totalPages))
        );
        return withState(DocumentStatus.PROCESSING, ProcessingStage.OCR_PROCESSING, percent, currentPage, totalPages,
                resultId, errorCode, errorMessage, now);
    }

    /**
     * Marks the document as completed.
     *
     * @param resultId OCR result id
     * @param now current time
     * @return updated document
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public DocumentJob complete(String resultId, OffsetDateTime now) {
        return withState(DocumentStatus.COMPLETED, ProcessingStage.OCR_COMPLETED, DocLensConstants.COMPLETED_PROGRESS_PERCENT, totalPages, totalPages,
                Optional.of(resultId), Optional.empty(), Optional.empty(), now);
    }

    /**
     * Marks the document as failed.
     *
     * @param code error code
     * @param message error message
     * @param now current time
     * @return updated document
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public DocumentJob fail(String code, String message, OffsetDateTime now) {
        return withState(DocumentStatus.FAILED, ProcessingStage.OCR_FAILED, DocLensConstants.COMPLETED_PROGRESS_PERCENT, currentPage, totalPages, resultId,
                Optional.of(code), Optional.ofNullable(message), now);
    }

    private DocumentJob withState(
            DocumentStatus nextStatus,
            ProcessingStage nextStage,
            int nextProgress,
            int nextCurrentPage,
            int nextTotalPages,
            Optional<String> nextResultId,
            Optional<String> nextErrorCode,
            Optional<String> nextErrorMessage,
            OffsetDateTime now
    ) {
        return new DocumentJob(documentId, batchId, fileName, fileType, fileSize, pageCount, storageUri, nextStatus,
                nextStage, nextProgress, nextCurrentPage, nextTotalPages, adapterName, pdfMode, metadata, nextResultId,
                nextErrorCode, nextErrorMessage, sortOrder, createdAt, now);
    }
}
