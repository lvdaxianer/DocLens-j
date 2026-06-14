CREATE TABLE IF NOT EXISTS ocr_document_page_results (
    page_result_id VARCHAR(180) PRIMARY KEY,
    document_id VARCHAR(80) NOT NULL,
    page_no INTEGER NOT NULL,
    raw_output TEXT NOT NULL,
    page_text TEXT NOT NULL,
    layout_blocks TEXT NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    warnings TEXT NOT NULL,
    node_id VARCHAR(80),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (document_id, page_no)
);

CREATE INDEX IF NOT EXISTS idx_ocr_page_results_document_page
    ON ocr_document_page_results (document_id, page_no);
