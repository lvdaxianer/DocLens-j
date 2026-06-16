CREATE TABLE IF NOT EXISTS ocr_batches (
    batch_id VARCHAR(80) PRIMARY KEY,
    status VARCHAR(40) NOT NULL,
    total_files INTEGER NOT NULL,
    completed_files INTEGER NOT NULL,
    failed_files INTEGER NOT NULL,
    current_document_id VARCHAR(80),
    current_document_name VARCHAR(512),
    current_stage VARCHAR(80),
    metadata TEXT NOT NULL,
    callback_url VARCHAR(2048),
    idempotency_key VARCHAR(256),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS ocr_documents (
    document_id VARCHAR(80) PRIMARY KEY,
    batch_id VARCHAR(80) NOT NULL,
    file_name VARCHAR(512) NOT NULL,
    file_type VARCHAR(40) NOT NULL,
    file_size BIGINT NOT NULL,
    page_count INTEGER NOT NULL,
    storage_uri VARCHAR(2048) NOT NULL,
    status VARCHAR(40) NOT NULL,
    stage VARCHAR(80) NOT NULL,
    progress_percent INTEGER NOT NULL,
    current_page INTEGER NOT NULL,
    total_pages INTEGER NOT NULL,
    adapter_name VARCHAR(120),
    pdf_mode VARCHAR(80),
    metadata TEXT NOT NULL,
    result_id VARCHAR(80),
    error_code VARCHAR(120),
    error_message TEXT,
    sort_order INTEGER NOT NULL,
    locked_by VARCHAR(120),
    locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ocr_documents_batch_sort
    ON ocr_documents (batch_id, sort_order, document_id);

CREATE TABLE IF NOT EXISTS ocr_results (
    result_id VARCHAR(80) PRIMARY KEY,
    document_id VARCHAR(80) NOT NULL UNIQUE,
    raw_vendor_output TEXT NOT NULL,
    structured_document TEXT NOT NULL,
    page_text TEXT NOT NULL,
    layout_blocks TEXT NOT NULL,
    tables TEXT NOT NULL,
    images TEXT NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    warnings TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS ocr_events (
    event_id VARCHAR(80) PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    batch_id VARCHAR(80) NOT NULL,
    document_id VARCHAR(80),
    status VARCHAR(40) NOT NULL,
    stage VARCHAR(80) NOT NULL,
    progress TEXT NOT NULL,
    metadata TEXT NOT NULL,
    result_id VARCHAR(80),
    result_summary TEXT,
    error TEXT,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ocr_events_batch_time
    ON ocr_events (batch_id, occurred_at);

CREATE TABLE IF NOT EXISTS ocr_callback_jobs (
    callback_job_id VARCHAR(80) PRIMARY KEY,
    event_id VARCHAR(80) NOT NULL,
    batch_id VARCHAR(80) NOT NULL,
    document_id VARCHAR(80),
    callback_url VARCHAR(2048) NOT NULL,
    status VARCHAR(40) NOT NULL,
    retry_count INTEGER NOT NULL,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    last_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
