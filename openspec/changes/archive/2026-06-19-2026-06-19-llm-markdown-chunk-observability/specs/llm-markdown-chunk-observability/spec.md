## ADDED Requirements

### Requirement: Batch detail SHALL surface chunked Markdown scale as a stable chunk count
The batch detail view SHALL expose the planned chunk count for chunked Markdown
documents so users can see how much work the LLM path is doing.

#### Scenario: Chunked document appears in batch detail
- **WHEN** a document row represents a chunked Markdown result
- **THEN** the row includes a stable chunk count value derived from `llm_chunk_count`
- **AND** the dashboard renders that value on the batch detail page
- **AND** the existing stage label and progress semantics remain unchanged

### Requirement: Dashboard OCR metrics SHALL expose the LLM chunk executor activity
The dashboard OCR resource panel SHALL show the shared LLM Markdown chunk executor
activity so users can tell whether chunk processing is actively running or queued.

#### Scenario: OCR resource panel renders chunk executor metrics
- **WHEN** the dashboard loads OCR resource metrics
- **THEN** the payload includes the LLM Markdown chunk executor active count and queue size
- **AND** the dashboard renders those values in a dedicated metric card
- **AND** existing OCR request and health queue metrics remain unchanged
