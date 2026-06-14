CREATE TABLE IF NOT EXISTS doclens_model_health (
    health_key VARCHAR(64) NOT NULL PRIMARY KEY,
    target_type VARCHAR(64) NOT NULL,
    model_key VARCHAR(128) NOT NULL,
    target_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    consecutive_failures BIGINT NOT NULL DEFAULT 0,
    consecutive_successes BIGINT NOT NULL DEFAULT 0,
    last_heartbeat_at TIMESTAMP WITH TIME ZONE,
    last_success_at TIMESTAMP WITH TIME ZONE,
    last_failure_at TIMESTAMP WITH TIME ZONE,
    last_failure_type VARCHAR(64),
    last_error VARCHAR(1024),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_doclens_model_health_target UNIQUE (target_type, model_key, target_id)
);

CREATE INDEX IF NOT EXISTS idx_doclens_model_health_status
    ON doclens_model_health(status);

CREATE INDEX IF NOT EXISTS idx_doclens_model_health_updated_at
    ON doclens_model_health(updated_at);
