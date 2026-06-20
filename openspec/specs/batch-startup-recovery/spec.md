# batch-startup-recovery Specification

## Purpose
Ensure DocLens can safely resume persisted batch work after a service restart by resubmitting batches that still have queued documents while leaving terminal and manually retried states untouched.
## Requirements
### Requirement: Service startup resubmits batches with queued documents

When automatic processing is enabled, DocLens MUST resubmit persisted batches that still have queued or startup-recoverable stalled documents after the service starts.

#### Scenario: Startup finds queued documents from a previously interrupted batch

- **GIVEN** a batch has one or more persisted documents in `QUEUED` status
- **AND** automatic upload processing is enabled
- **WHEN** the application startup recovery runner executes
- **THEN** the owning batch ID is submitted to `BatchProcessingScheduler`
- **AND** each batch ID is submitted at most once per recovery scan
- **AND** normal batch processing continues by processing only queued documents

#### Scenario: Startup recovers stalled documents from a previously interrupted batch

- **GIVEN** a batch has one or more persisted documents in `STALLED` status
- **AND** automatic upload processing is enabled
- **WHEN** the application startup recovery runner executes
- **THEN** each stalled document in that recovered batch is reset to `QUEUED`
- **AND** stale page tasks and page results for those documents are cleared before reprocessing
- **AND** the owning batch ID is submitted to `BatchProcessingScheduler` at most once
- **AND** normal batch processing continues by processing the reset queued documents

#### Scenario: Startup does not automatically retry terminal failed documents

- **GIVEN** a batch has no persisted documents in `QUEUED` or `STALLED` status
- **AND** the batch contains only `COMPLETED` or `FAILED` documents
- **WHEN** the application startup recovery runner executes
- **THEN** that batch is not submitted to `BatchProcessingScheduler`
- **AND** failed documents still require the existing manual retry flow

#### Scenario: Startup recovery is bounded

- **GIVEN** more recoverable batches exist than the configured startup recovery limit
- **WHEN** the application startup recovery runner executes
- **THEN** no more than the configured limit of distinct batch IDs is submitted
- **AND** the recovery log records the number of submitted batches

#### Scenario: Automatic processing disabled leaves recoverable batches untouched

- **GIVEN** automatic upload processing is disabled
- **WHEN** the application startup recovery runner would otherwise execute
- **THEN** queued and stalled batches are not submitted automatically
- **AND** external workers or manual operations remain responsible for scheduling them

