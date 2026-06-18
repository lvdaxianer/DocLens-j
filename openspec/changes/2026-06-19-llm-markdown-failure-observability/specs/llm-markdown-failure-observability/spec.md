## MODIFIED Requirements

### Requirement: Preserve Markdown failure reason
The system SHALL preserve the concrete root cause of Markdown post-processing failures when the LLM call fails and the OCR pipeline falls back to OCR text.

#### Scenario: LLM request fails after retries
- **WHEN** the HTTP Markdown post-processor throws a transport, timeout, or upstream error
- **AND** the Markdown post-processing service exhausts its retries
- **THEN** the saved OCR result includes a concrete `llm_error_message` that reflects the root cause rather than a generic wrapper message
- **AND** the success path and prompt behavior remain unchanged
