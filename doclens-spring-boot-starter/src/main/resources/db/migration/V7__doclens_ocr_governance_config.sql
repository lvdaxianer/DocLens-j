CREATE TABLE IF NOT EXISTS doclens_ocr_governance_config (
    id VARCHAR(80) PRIMARY KEY,
    failure_threshold INT NOT NULL,
    probe_interval_seconds INT NOT NULL,
    circuit_open_seconds INT NOT NULL,
    recovery_success_threshold INT NOT NULL,
    manual_recovery_attempts INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
