# llm-markdown-always-apply Specification

## Purpose
Ensure DocLens applies configured LLM Markdown formatting to every OCR result,
without treating legacy orchestration metadata as a processing-time skip signal.
## Requirements
### Requirement: Configured LLM Markdown applies to every OCR result

When a usable LLM Markdown configuration exists, DocLens MUST invoke Markdown post-processing for OCR results regardless of source file format, legacy orchestration metadata, or document processing path. The page-task aggregation path MUST follow the same LLM Markdown application and fallback semantics as the batch result builder path.

#### Scenario: Legacy orchestration flag does not skip LLM formatting

- **GIVEN** a document was created with legacy LLM orchestration metadata set to true
- **AND** a usable Markdown post-processor is configured
- **WHEN** the document OCR result is built
- **THEN** the final text is the Markdown post-processor output
- **AND** the stored raw output contains `llm_markdown_applied=true`

#### Scenario: Page-task aggregation applies configured Markdown post-processing

- **GIVEN** a document was processed through page-task OCR and all page tasks
  completed
- **AND** the runtime Markdown post-processor returns Markdown content
- **WHEN** the page-task aggregation service saves the final OCR result
- **THEN** the saved final text uses the Markdown content
- **AND** the stored raw output contains `llm_markdown_applied=true`
- **AND** the stored raw output preserves the original OCR text under `ocr_text`

#### Scenario: Page-task aggregation preserves LLM metadata

- **GIVEN** a document was processed through page-task OCR
- **AND** Markdown post-processing returns observability metadata such as
  `llm_chunk_count`
- **WHEN** the final OCR result is saved
- **THEN** the raw output includes that LLM metadata

#### Scenario: Page-task aggregation exposes LLM fallback

- **GIVEN** a document was processed through page-task OCR
- **AND** Markdown post-processing fails after its retries
- **WHEN** the final OCR result is saved
- **THEN** the saved final text falls back to the OCR text
- **AND** warnings include `llm_markdown_post_processing_failed`
- **AND** raw output contains `llm_markdown_applied=false`
- **AND** raw output contains the final LLM failure message when available
