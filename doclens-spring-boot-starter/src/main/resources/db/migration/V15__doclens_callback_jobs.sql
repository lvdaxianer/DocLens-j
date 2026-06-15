ALTER TABLE ocr_callback_jobs
    ADD COLUMN IF NOT EXISTS payload TEXT NOT NULL DEFAULT '{}';

ALTER TABLE ocr_callback_jobs
    ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(120);

ALTER TABLE ocr_callback_jobs
    ADD COLUMN IF NOT EXISTS failure_detail TEXT;

CREATE INDEX IF NOT EXISTS idx_ocr_callback_jobs_status_retry
    ON ocr_callback_jobs (status, next_retry_at, updated_at, callback_job_id);
