ALTER TABLE doclens_llm_markdown_config
    ADD COLUMN IF NOT EXISTS max_context_tokens INTEGER NOT NULL DEFAULT 16000;

ALTER TABLE doclens_llm_markdown_config
    ADD COLUMN IF NOT EXISTS max_concurrency INTEGER NOT NULL DEFAULT 1;

ALTER TABLE doclens_llm_markdown_config
    ADD COLUMN IF NOT EXISTS request_interval_millis INTEGER NOT NULL DEFAULT 1000;
