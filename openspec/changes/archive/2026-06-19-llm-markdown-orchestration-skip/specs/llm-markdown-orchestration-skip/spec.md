# llm-markdown-orchestration-skip Specification

## ADDED Requirements

### Requirement: Uploads can declare upstream orchestration
The upload request MUST accept a boolean `llmOrchestrated` field that indicates whether the document has already been LLM-orchestrated upstream.

#### Scenario: Flag is omitted
- **WHEN** an upload request does not include `llmOrchestrated`
- **THEN** the backend MUST treat it as `false`
- **AND** the document must follow the existing Markdown post-processing path

#### Scenario: Flag is enabled
- **WHEN** an upload request includes `llmOrchestrated=true`
- **THEN** the backend MUST preserve that flag on the created document job
- **AND** the upload contract must not require any additional orchestration fields

### Requirement: Already-orchestrated uploads skip Markdown post-processing
When a document job is marked `llmOrchestrated=true`, DocLens MUST skip its own Markdown post-processing step and preserve the extracted OCR text as the final text.

#### Scenario: Skip post-processing
- **WHEN** a document job with `llmOrchestrated=true` reaches Markdown result building
- **THEN** DocLens MUST not invoke the Markdown post-processor
- **AND** the resulting OCR result MUST keep the extracted OCR text as the final text
- **AND** the result metadata MUST indicate that Markdown was not applied

### Requirement: Default behavior remains unchanged
Documents that do not opt into the skip flag MUST continue to use the existing Markdown post-processing path.

#### Scenario: Default processing still applies Markdown
- **WHEN** a document job has `llmOrchestrated=false`
- **THEN** DocLens MUST invoke the Markdown post-processor as it does today
- **AND** the final result behavior MUST remain unchanged from the current implementation
