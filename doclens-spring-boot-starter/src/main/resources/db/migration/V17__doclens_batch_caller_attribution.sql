ALTER TABLE ocr_batches
    ADD COLUMN IF NOT EXISTS client_id VARCHAR(120) NOT NULL DEFAULT 'anonymous';

ALTER TABLE ocr_batches
    ADD COLUMN IF NOT EXISTS source_app VARCHAR(160) NOT NULL DEFAULT 'unknown';

ALTER TABLE ocr_batches
    ADD COLUMN IF NOT EXISTS tenant_key VARCHAR(160);

CREATE INDEX IF NOT EXISTS idx_ocr_batches_caller_updated
    ON ocr_batches (client_id, updated_at);
