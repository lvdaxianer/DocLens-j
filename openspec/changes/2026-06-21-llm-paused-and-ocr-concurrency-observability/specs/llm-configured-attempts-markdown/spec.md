## ADDED Requirements

### Requirement: Paused LLM configs are reported as paused passthrough

DocLens MUST return OCR text without attempting credential resolution or provider
calls, and MUST mark the result as paused rather than failed, when Markdown
post-processing configs exist for the Markdown usage but all complete configs are
disabled.

#### Scenario: All Markdown LLM configs are paused

- **GIVEN** at least one Markdown LLM configuration exists with URL, model, and
  credential reference
- **AND** every complete Markdown LLM configuration is disabled
- **WHEN** OCR result building runs Markdown post-processing
- **THEN** no LLM credential is resolved
- **AND** no LLM provider call is attempted
- **AND** the result warning includes `llm_markdown_paused`
- **AND** the result warning does not include `llm_markdown_post_processing_failed`

#### Scenario: Enabled config still reports real attempt failure

- **GIVEN** a Markdown LLM configuration is complete and enabled
- **AND** its credential environment variable is missing
- **WHEN** OCR result building runs Markdown post-processing
- **THEN** DocLens attempts to create/use the runtime LLM processor
- **AND** the result warning includes `llm_markdown_post_processing_failed`
- **AND** the result exposes the credential failure reason
