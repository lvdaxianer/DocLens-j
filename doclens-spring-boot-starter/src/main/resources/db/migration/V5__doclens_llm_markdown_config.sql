CREATE TABLE IF NOT EXISTS doclens_llm_markdown_config (
    id VARCHAR(80) PRIMARY KEY,
    url VARCHAR(2048) NOT NULL,
    model VARCHAR(128) NOT NULL,
    credential_ref VARCHAR(2000),
    credential_configured BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
