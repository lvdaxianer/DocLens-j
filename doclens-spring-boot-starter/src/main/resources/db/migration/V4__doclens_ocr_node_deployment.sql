ALTER TABLE doclens_ocr_nodes
    ADD COLUMN IF NOT EXISTS deployment_type VARCHAR(32) NOT NULL DEFAULT 'OFFLINE';

ALTER TABLE doclens_ocr_nodes
    ADD COLUMN IF NOT EXISTS channel_key VARCHAR(64);

ALTER TABLE doclens_ocr_nodes
    ADD COLUMN IF NOT EXISTS provider_model VARCHAR(128);

ALTER TABLE doclens_ocr_nodes
    ADD COLUMN IF NOT EXISTS credential_ref VARCHAR(2000);

ALTER TABLE doclens_ocr_nodes
    ADD COLUMN IF NOT EXISTS credential_configured BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE doclens_ocr_nodes
    DROP CONSTRAINT IF EXISTS uk_doclens_ocr_nodes_model_host_port;

CREATE INDEX IF NOT EXISTS idx_doclens_ocr_nodes_deployment
    ON doclens_ocr_nodes (deployment_type, channel_key);
