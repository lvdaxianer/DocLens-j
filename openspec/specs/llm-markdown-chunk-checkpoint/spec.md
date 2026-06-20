# llm-markdown-chunk-checkpoint Specification

## Purpose
Define durable chunk-level checkpoint behavior for LLM Markdown
post-processing, including filesystem persistence, retry reuse, stale
checkpoint rejection, async write completion, and explicit delete cleanup.

## Requirements
### Requirement: Successful Markdown chunks SHALL be checkpointed to disk

The system SHALL persist successful LLM Markdown chunk outputs under the
configured storage root using deterministic per-document and per-chunk paths.

#### Scenario: Successful chunk is saved with ordered path

- **WHEN** a document chunk is successfully formatted by the LLM
- **THEN** the system writes the chunk Markdown to
  `llm-markdown-chunks/<documentId>/<chunkNo>/README.md`
- **AND** the system writes chunk metadata to
  `llm-markdown-chunks/<documentId>/<chunkNo>/meta.json`
- **AND** the chunk number is zero-padded to at least two digits and to enough
  digits for the total chunk count

#### Scenario: Fallback chunk is not checkpointed

- **WHEN** a chunk exhausts retries and falls back to original OCR text
- **THEN** the system uses that text for the current in-memory document join
- **AND** the system does not write that fallback result as a successful
  checkpoint

### Requirement: Markdown chunk retries SHALL reuse valid checkpoints

The system SHALL load checkpointed chunks during document retry or startup
recovery and skip LLM calls for chunks that match the current document chunk
plan.

#### Scenario: Valid checkpoint skips LLM call

- **WHEN** a chunk checkpoint exists with a matching manifest, valid metadata,
  and non-empty README content
- **THEN** the system returns the checkpointed Markdown for that chunk
- **AND** the system does not call the LLM delegate for that chunk

#### Scenario: Missing chunk is processed normally

- **WHEN** some chunks have valid checkpoints and another chunk is missing
- **THEN** the system reuses the checkpointed chunks
- **AND** the system calls the LLM delegate only for the missing chunk
- **AND** the final Markdown output is joined in the original chunk order

### Requirement: Stale or partial checkpoints SHALL NOT be reused

The system SHALL validate checkpoint identity and chunk completeness before
using checkpointed Markdown.

#### Scenario: Plan identity changes

- **WHEN** a checkpoint manifest does not match the current chunk plan identity
- **THEN** the system ignores the checkpoint directory for the current run
- **AND** the system processes chunks through the LLM as needed

#### Scenario: Chunk file is partial or empty

- **WHEN** a chunk directory exists but its `README.md` is empty, missing, or
  inconsistent with metadata
- **THEN** the system treats that chunk as incomplete
- **AND** the system processes the chunk through the LLM as needed

### Requirement: Checkpoint writes SHALL be asynchronous but completed before document success returns

The system SHALL schedule chunk checkpoint writes asynchronously after a chunk
succeeds, and SHALL wait for the current run's pending checkpoint writes before
returning the final document Markdown result.

#### Scenario: Checkpoint write is asynchronous

- **WHEN** a chunk returns successful LLM Markdown
- **THEN** the processor schedules the checkpoint save without blocking the next
  chunk LLM work on that disk write

#### Scenario: Final document waits for pending writes

- **WHEN** all chunk results have been gathered for a document
- **THEN** the processor waits for pending checkpoint write futures from the
  current run before returning the final Markdown result
- **AND** a document that has completed normally has all successfully written
  chunks durably available for later retry

### Requirement: Checkpoints SHALL be cleaned up with deleted documents

The system SHALL remove checkpoint directories when a document or its containing
batch is explicitly deleted.

#### Scenario: Document delete removes checkpoint directory

- **WHEN** a document is deleted through the existing delete use case
- **THEN** the system deletes
  `llm-markdown-chunks/<documentId>/` if it exists

#### Scenario: Batch delete removes checkpoint directories

- **WHEN** a batch is deleted through the existing batch delete use case
- **THEN** the system deletes checkpoint directories for the batch's documents
  if they exist
