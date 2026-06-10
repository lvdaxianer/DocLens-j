ALTER TABLE doclens_llm_markdown_config
    ADD COLUMN IF NOT EXISTS api_type VARCHAR(32) NOT NULL DEFAULT 'openai';

ALTER TABLE doclens_llm_markdown_config
    ADD COLUMN IF NOT EXISTS healthy BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE doclens_llm_markdown_config
    ADD COLUMN IF NOT EXISTS health_message VARCHAR(2000) NOT NULL DEFAULT '';

ALTER TABLE doclens_llm_markdown_config
    ADD COLUMN IF NOT EXISTS last_health_at TIMESTAMP WITH TIME ZONE;
