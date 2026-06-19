## ADDED Requirements

### Requirement: Markdown chunks are processed in parallel across a shared worker pool

The system SHALL treat each Markdown chunk as an independent work item and
submit those work items to a shared worker pool so chunks from the same document
and chunks from different documents can run concurrently.

#### Scenario: One large document uses multiple workers

- **WHEN** a Markdown document is split into multiple chunks
- **THEN** the system submits every chunk to the shared worker pool without
  waiting for the previous chunk to finish first
- **AND** the system preserves the original chunk order when joining the final
  Markdown text

#### Scenario: Two documents share the same chunk pool

- **WHEN** two Markdown documents are processed at the same time
- **THEN** chunk tasks from both documents can be scheduled against the same
  worker pool concurrently
- **AND** one document does not have to finish its chunk sequence before the
  other document's chunk sequence begins

### Requirement: Markdown chunk work keeps round-robin config selection and per-config limits

Each Markdown chunk work item SHALL select a healthy LLM Markdown configuration
at execution time using the existing round-robin selection rules, and SHALL
continue to honor the selected configuration's concurrency limit and request
interval.

#### Scenario: Healthy configs are still selected in round-robin order

- **WHEN** multiple healthy LLM Markdown configurations are available
- **THEN** each chunk task selects from the same ordered healthy pool using the
  existing round-robin behavior
- **AND** the system does not permanently pin all chunk work to a single
  configuration unless only one healthy configuration exists

#### Scenario: Per-config limits remain enforced

- **WHEN** many chunk tasks select the same LLM Markdown configuration
- **THEN** the configuration's concurrency limit and request interval still gate
  those chunk requests

### Requirement: Failed chunks fall back to original chunk text and the document continues

If a chunk exhausts its retries or otherwise fails permanently, the system SHALL
fall back to that chunk's original text and continue joining the remaining chunk
results.

#### Scenario: One chunk fails after retries

- **WHEN** one chunk exhausts its retries and still fails
- **THEN** the system uses that chunk's original OCR text in the final join
- **AND** successful chunks keep their Markdown output
- **AND** the document still completes instead of failing the whole batch

### Requirement: Chunk-level parallelism remains internal to the synchronous API

The public Markdown post-processing API SHALL remain synchronous to its callers
while using the shared worker pool internally.

#### Scenario: Existing callers still invoke a synchronous API

- **WHEN** existing batch processing code calls the Markdown post-processor
- **THEN** it still receives a single synchronous result for the document
- **AND** the chunk-level concurrency remains an internal detail
