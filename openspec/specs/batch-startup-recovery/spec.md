# batch-startup-recovery Specification

## Purpose
TBD - created by archiving change 2026-06-20-batch-startup-recovery. Update Purpose after archive.
## Requirements
### Requirement: Service startup resubmits batches with queued documents

When automatic processing is enabled, DocLens MUST resubmit persisted batches that still have queued documents after the service starts.

#### Scenario: Startup finds queued documents from a previously interrupted batch

- **GIVEN** a batch has one or more persisted documents in `QUEUED` status
- **AND** automatic upload processing is enabled
- **WHEN** the application startup recovery runner executes
- **THEN** the owning batch ID is submitted to `BatchProcessingScheduler`
- **AND** each batch ID is submitted at most once per recovery scan
- **AND** normal batch processing continues by processing only queued documents

#### Scenario: Startup does not automatically retry terminal or stalled documents

- **GIVEN** a batch has no persisted documents in `QUEUED` status
- **AND** the batch contains only `COMPLETED`, `FAILED`, or `STALLED` documents
- **WHEN** the application startup recovery runner executes
- **THEN** that batch is not submitted to `BatchProcessingScheduler`
- **AND** failed or stalled documents still require the existing manual retry flow

#### Scenario: Startup recovery is bounded

- **GIVEN** more queued batches exist than the configured startup recovery limit
- **WHEN** the application startup recovery runner executes
- **THEN** no more than the configured limit of distinct batch IDs is submitted
- **AND** the recovery log records the number of submitted batches

#### Scenario: Automatic processing disabled leaves queued batches untouched

- **GIVEN** automatic upload processing is disabled
- **WHEN** the application startup recovery runner would otherwise execute
- **THEN** queued batches are not submitted automatically
- **AND** external workers or manual operations remain responsible for scheduling them

