# ocr-page-concurrency-visibility Specification

## Purpose
TBD - created by archiving change 2026-06-21-ocr-page-concurrency-visibility. Update Purpose after archive.
## Requirements
### Requirement: Batch detail exposes live OCR page task concurrency

The Dashboard batch detail read model SHALL expose currently in-flight OCR page
tasks with enough detail to prove which image page is being consumed by which
local worker thread.

#### Scenario: Two pages are running concurrently

- **GIVEN** one document has page 1 and page 2 currently executing OCR
- **AND** the pages are executing on different local worker threads
- **WHEN** the Dashboard loads the batch detail
- **THEN** the response includes two `ocr_running_page_tasks` rows
- **AND** each row includes document ID, page number, worker ID, thread name,
  started time, running duration, OCR model, and OCR node
- **AND** the two rows preserve their distinct thread names

#### Scenario: OCR has completed and later stages are running

- **GIVEN** all OCR page tasks for a document have completed
- **AND** the document is currently in a later stage such as LLM Markdown
- **WHEN** the Dashboard loads the batch detail
- **THEN** `ocr_running_page_tasks` contains no rows for that document
- **AND** the final OCR allocation summary remains available for completed OCR
  page ownership

### Requirement: Live OCR page task observability must not block processing

The live page task tracker SHALL be best-effort runtime observability and SHALL
not fail document processing if runtime tracking cannot publish a row.

#### Scenario: Live tracking cleanup runs after OCR failure

- **GIVEN** a page task is recorded as running
- **WHEN** the OCR request fails and the page task is marked failed
- **THEN** the live page task row is removed
- **AND** the normal failure handling continues

