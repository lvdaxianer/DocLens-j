ALTER TABLE doclens_llm_markdown_config
    ADD COLUMN IF NOT EXISTS name VARCHAR(128) NOT NULL DEFAULT '默认 LLM 配置';

ALTER TABLE doclens_llm_markdown_config
    ADD COLUMN IF NOT EXISTS usage_type VARCHAR(64) NOT NULL DEFAULT 'MARKDOWN_POST_PROCESSING';

ALTER TABLE doclens_llm_markdown_config
    ADD COLUMN IF NOT EXISTS priority INTEGER NOT NULL DEFAULT 100;

ALTER TABLE doclens_llm_markdown_config
    ADD COLUMN IF NOT EXISTS is_default BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE doclens_llm_markdown_config
SET is_default = TRUE
WHERE id = 'default'
  AND usage_type = 'MARKDOWN_POST_PROCESSING';
