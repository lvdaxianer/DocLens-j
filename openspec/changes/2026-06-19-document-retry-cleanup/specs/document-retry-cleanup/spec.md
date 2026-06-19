## ADDED Requirements

### Requirement: Document retry clears old page-level children

The document retry flow MUST remove the target document's existing page tasks and page OCR results before the document is queued again.

#### Scenario: Retry restarts a failed document from a clean child state

- **WHEN** an operator retries a failed or stalled document that already has page tasks and page OCR results stored for that document
- **THEN** the retry flow deletes the existing page tasks for that document
- **AND** the retry flow deletes the existing page OCR results for that document
- **AND** the retry flow resets the document job back to a retryable queued state
- **AND** the retry flow reschedules the owning batch without hitting page-level unique-key conflicts
