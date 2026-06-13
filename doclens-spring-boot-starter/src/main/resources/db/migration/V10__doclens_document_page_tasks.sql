CREATE TABLE IF NOT EXISTS ocr_document_page_tasks (
    task_id VARCHAR(80) PRIMARY KEY,
    batch_id VARCHAR(80) NOT NULL,
    document_id VARCHAR(80) NOT NULL,
    page_no INTEGER NOT NULL,
    image_storage_uri VARCHAR(2048) NOT NULL,
    status VARCHAR(40) NOT NULL,
    locked_by VARCHAR(120),
    locked_until TIMESTAMP WITH TIME ZONE,
    retry_count INTEGER NOT NULL,
    error_code VARCHAR(120),
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (document_id, page_no)
);

CREATE INDEX IF NOT EXISTS idx_ocr_page_tasks_status_created
    ON ocr_document_page_tasks (status, created_at, task_id);

CREATE INDEX IF NOT EXISTS idx_ocr_page_tasks_document_page
    ON ocr_document_page_tasks (document_id, page_no);
