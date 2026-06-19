# llm-markdown-always-apply Specification

## Purpose
Ensure DocLens applies configured LLM Markdown formatting to every OCR result,
without treating legacy orchestration metadata as a processing-time skip signal.
## Requirements
### Requirement: Configured LLM Markdown applies to every OCR result

When a usable LLM Markdown configuration exists, DocLens MUST invoke Markdown post-processing for OCR results regardless of source file format or legacy orchestration metadata.

#### Scenario: Legacy orchestration flag does not skip LLM formatting

- **GIVEN** a document was created with legacy LLM orchestration metadata set to true
- **AND** a usable Markdown post-processor is configured
- **WHEN** the document OCR result is built
- **THEN** the final text is the Markdown post-processor output
- **AND** the stored raw output contains `llm_markdown_applied=true`
