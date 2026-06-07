package io.github.lvdaxianer.doclens.j.shared.domain;

/**
 * Shared constants for DocLens domain and API contracts.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public final class DocLensConstants {

    public static final int ZERO_PROGRESS_PERCENT = 0;
    public static final int START_PROGRESS_PERCENT = 5;
    public static final int MAX_PROCESSING_PROGRESS_PERCENT = 95;
    public static final int COMPLETED_PROGRESS_PERCENT = 100;
    public static final int DEFAULT_PAGE_NO = 1;
    public static final long FIRST_PAGE_NO = 1L;
    public static final long SINGLE_QUERY_LIMIT = 1L;
    public static final int DEFAULT_PAGE_COUNT = 1;
    public static final int DEFAULT_BLOCK_COUNT = 1;
    public static final int DEFAULT_TABLE_COUNT = 0;
    public static final int DEFAULT_QUERY_LIMIT = 10000;
    public static final double STUB_CONFIDENCE = 0.99D;
    public static final String DEFAULT_ADAPTER_KEY = "paddle_ocr";
    public static final String EMPTY_VALUE = "";
    public static final String STAGE_QUEUED = "queued";
    public static final String EVENT_BATCH_CREATED = "batch.created";
    public static final String EVENT_DOCUMENT_ENQUEUED = "document.enqueued";
    public static final String EVENT_DOCUMENT_STARTED = "document.started";
    public static final String EVENT_DOCUMENT_PAGE_COMPLETED = "document.page.completed";
    public static final String EVENT_DOCUMENT_COMPLETED = "document.completed";
    public static final String EVENT_DOCUMENT_FAILED = "document.failed";
    public static final String ERROR_CODE_OCR_FAILED = "OCR_FAILED";

    private DocLensConstants() {
    }
}
