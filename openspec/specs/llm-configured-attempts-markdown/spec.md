# llm-configured-attempts-markdown Specification

## Purpose
Ensure DocLens attempts LLM Markdown formatting whenever a complete LLM config is
enabled, while keeping health status as observability rather than a processing
gate.
## Requirements
### Requirement: Enabled configured LLMs are attempted for Markdown

When an LLM Markdown configuration is complete and enabled, DocLens MUST attempt to
use it for Markdown post-processing even if its latest health status is unhealthy.

#### Scenario: Last health check is unhealthy

- **GIVEN** an LLM Markdown configuration has URL, model, credential, and is enabled
- **AND** its latest health status is unhealthy
- **WHEN** OCR result building selects a Markdown post-processing config
- **THEN** the config is eligible for selection
- **AND** the actual LLM call determines whether the result is applied or falls back

#### Scenario: Config is disabled or incomplete

- **GIVEN** an LLM Markdown configuration is disabled or incomplete
- **WHEN** OCR result building selects a Markdown post-processing config
- **THEN** the config is not eligible for selection

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

### Requirement: LLM credential environment variable guidance is explicit

The Dashboard LLM configuration experience MUST explain that credential input is an environment variable name resolved by the backend service process.

#### Scenario: Operator edits an LLM config

- **WHEN** the operator opens the LLM config drawer
- **THEN** the credential field label and helper text explain that the value is an environment variable name
- **AND** the UI states that the environment variable must be set before starting or restarting the backend service

#### Scenario: Runtime LLM fails because the env var is missing

- **WHEN** a document result contains a credential environment variable missing error
- **THEN** the result drawer displays a Chinese actionable message naming the missing environment variable
- **AND** the message tells the operator to configure the backend service environment and restart the service

