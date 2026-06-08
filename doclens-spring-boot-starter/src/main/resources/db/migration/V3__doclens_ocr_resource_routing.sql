CREATE TABLE IF NOT EXISTS doclens_ocr_nodes (
    id VARCHAR(80) PRIMARY KEY,
    model_key VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    host VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL,
    participate_global BOOLEAN NOT NULL,
    weight INTEGER NOT NULL,
    max_concurrency INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    failure_count BIGINT NOT NULL,
    success_count BIGINT NOT NULL,
    avg_latency_ms BIGINT NOT NULL,
    p95_latency_ms BIGINT NOT NULL,
    last_health_at TIMESTAMP WITH TIME ZONE,
    last_success_at TIMESTAMP WITH TIME ZONE,
    last_failure_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_doclens_ocr_nodes_model_host_port UNIQUE (model_key, host, port)
);

CREATE INDEX IF NOT EXISTS idx_doclens_ocr_nodes_model_status
    ON doclens_ocr_nodes (model_key, status);

CREATE TABLE IF NOT EXISTS doclens_ocr_node_calls (
    id VARCHAR(80) PRIMARY KEY,
    batch_id VARCHAR(80) NOT NULL,
    document_id VARCHAR(80) NOT NULL,
    page_no INTEGER NOT NULL,
    model_key VARCHAR(64) NOT NULL,
    node_id VARCHAR(80) NOT NULL,
    routing_mode VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INTEGER NOT NULL,
    elapsed_ms BIGINT NOT NULL,
    error_code VARCHAR(128),
    error_message VARCHAR(1000),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_doclens_ocr_node_calls_document
    ON doclens_ocr_node_calls (document_id, started_at);

CREATE INDEX IF NOT EXISTS idx_doclens_ocr_node_calls_node_time
    ON doclens_ocr_node_calls (node_id, started_at);

ALTER TABLE ocr_documents
    ADD COLUMN IF NOT EXISTS ocr_routing_mode VARCHAR(64);

ALTER TABLE ocr_documents
    ADD COLUMN IF NOT EXISTS ocr_model_key VARCHAR(64);

ALTER TABLE ocr_documents
    ADD COLUMN IF NOT EXISTS ocr_node_id VARCHAR(80);

ALTER TABLE ocr_documents
    ADD COLUMN IF NOT EXISTS ocr_load_balance_strategy VARCHAR(64);
