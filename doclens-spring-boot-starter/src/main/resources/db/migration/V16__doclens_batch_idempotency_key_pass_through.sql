ALTER TABLE ocr_batches
    DROP CONSTRAINT IF EXISTS "CONSTRAINT_86";

ALTER TABLE ocr_batches
    DROP CONSTRAINT IF EXISTS ocr_batches_idempotency_key_key;

ALTER TABLE ocr_batches
    DROP CONSTRAINT IF EXISTS uk_ocr_batches_idempotency_key;
