## Context

`BatchStartupRecoveryService.recoverQueuedBatches()` currently calls
`DocumentJobRepository.listQueuedBatchIds(limit)` and schedules those batches. This works
only while interrupted documents remain in `QUEUED` status. The dashboard issue shows
documents already marked `STALLED`, so the startup scan skips them and the later
`BatchProcessingUseCase.queuedDocuments()` filter would skip them even if their batch were
scheduled.

Manual document retry already has the correct reset behavior: it clears page tasks and
page results, calls `DocumentJob.retry(now)`, refreshes the batch summary, writes a retry
event, and schedules the batch. Startup recovery needs the same reset semantics, but
scoped only to stale/crash recovery rather than all failed documents.

## Goals / Non-Goals

**Goals:**
- On startup, recover batches that contain persisted `STALLED` documents.
- Reset startup-recovered stalled documents to `QUEUED` before scheduling so normal
  batch processing can pick them up.
- Clear page-level task/result children for recovered stalled documents to avoid retry
  collisions.
- Keep `FAILED` documents manual-only.
- Keep startup recovery bounded by the existing recovery limit.
- Add regression tests for service behavior and repository status lookup.

**Non-Goals:**
- Do not change dashboard display fields.
- Do not introduce distributed leases.
- Do not add a periodic scanner in this change.
- Do not retry completed or failed documents automatically.

## File-Level Implementation Plan

### Core contracts

- `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentJobRepository.java`
  - Add a status-based distinct batch lookup, e.g. `listBatchIdsByStatus(DocumentStatus status, int limit)`.
  - Keep `listQueuedBatchIds(int limit)` as a compatibility default delegating to the new method.

- `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/application/BatchStartupRecoveryService.java`
  - Add dependencies needed for reset semantics: page task cleanup, page result cleanup, batch repository,
    event repository/factory, and transaction runner.
  - Keep startup scheduling outside the transaction after documents are reset.
  - Recover queued batches as before.
  - Recover stalled batches by resetting each `STALLED` document in those batches with `DocumentJob.retry(now)`,
    cleaning page children, refreshing summaries, saving events, then scheduling each distinct batch once.
  - Do not reset `FAILED` documents.

### Infrastructure

- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentJobRepository.java`
  - Implement the new status-based distinct batch lookup with deterministic ordering and limit.

- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensProcessingAutoConfiguration.java`
  - Update the `BatchStartupRecoveryService` bean wiring with the new dependencies.

### Tests

- `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/ingestion/application/BatchStartupRecoveryServiceTest.java`
  - Add RED coverage that a stalled-only batch is reset to queued and scheduled on startup recovery.
  - Verify failed-only batches are still skipped.
  - Verify duplicate queued/stalled documents from the same batch schedule once.

- `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentJobRepositoryTest.java`
  - Add coverage for status-based distinct batch lookup, including `STALLED`.

- `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensProcessingAutoConfigurationTest.java`
  - Update wiring tests if constructor dependencies change.

## Data Flow

1. Application runner invokes batch startup recovery when automatic processing is enabled.
2. Recovery scans for distinct queued batch IDs and stalled batch IDs within the limit.
3. For stalled batch IDs, recovery loads the batch documents and selects only `STALLED` documents.
4. Each stalled document is reset to queued using existing retry domain behavior after page children are cleaned.
5. Batch summaries and recovery events are persisted.
6. Each affected batch is scheduled once through `BatchProcessingScheduler`.
7. `BatchProcessingUseCase` continues through the existing queued-document path.

## Risks / Trade-offs

- Resetting `STALLED` documents on startup can repeat OCR work after a long-running but not truly dead task. This is acceptable because `STALLED` is already the system's explicit "no progress beyond threshold" state.
- Cleaning page children discards partial page OCR data for recovered stalled documents. This matches manual retry behavior and avoids unique-key collisions.
- In multi-node deployments, another node could also recover the same stalled batch. The current system still relies on existing idempotent processing and page-task claim behavior; distributed leases remain a later design.

## Testing

- Run the focused `BatchStartupRecoveryServiceTest` RED/GREEN cycle.
- Run the focused MyBatis repository test for the new status-based lookup.
- Run auto-configuration tests that construct startup recovery.
- Run the relevant core/starter recovery slice.
- Run `openspec validate 2026-06-20-stalled-startup-recovery --strict`.
