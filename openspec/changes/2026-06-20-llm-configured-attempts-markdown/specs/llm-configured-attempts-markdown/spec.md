## ADDED Requirements

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
