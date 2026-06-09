ALTER TABLE doclens_ocr_nodes
    ADD COLUMN IF NOT EXISTS consecutive_failure_count BIGINT NOT NULL DEFAULT 0;

ALTER TABLE doclens_ocr_nodes
    ADD COLUMN IF NOT EXISTS recovery_success_count BIGINT NOT NULL DEFAULT 0;

ALTER TABLE doclens_ocr_nodes
    ADD COLUMN IF NOT EXISTS last_health_check_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE doclens_ocr_nodes
    ADD COLUMN IF NOT EXISTS circuit_open_until TIMESTAMP WITH TIME ZONE;

ALTER TABLE doclens_ocr_nodes
    ADD COLUMN IF NOT EXISTS last_manual_recovery_at TIMESTAMP WITH TIME ZONE;

UPDATE doclens_ocr_nodes
SET consecutive_failure_count = failure_count,
    recovery_success_count = success_count,
    last_health_check_at = COALESCE(last_health_at, last_success_at, last_failure_at)
WHERE consecutive_failure_count = 0
  AND recovery_success_count = 0
  AND last_health_check_at IS NULL;
