## Context

`CreateBatchUseCase.create()` persists the batch, document jobs, upload events, and object-storage bytes before calling `BatchProcessingScheduler.schedule(batchId)`. `BatchProcessingUseCase.processBatch()` is already idempotent enough for this recovery scope because it only reads `DocumentStatus.QUEUED` documents for a batch. If a document has already advanced beyond queued, the resubmitted batch will not reprocess it through this path.

The system also has `PageTaskWorkerScheduler`, which starts on application startup and recovers expired page-level `PROCESSING` locks before claiming queued page tasks. That closes the page OCR phase, but not the earlier batch/document preparation phase.

## Goals / Non-Goals

**Goals:**
- On application startup, automatically resubmit batches that still have queued documents.
- Avoid resubmitting completed, fully failed, or stalled-only batches.
- Bound each recovery scan so startup cannot enqueue an unbounded number of batches at once.
- Reuse the existing `BatchProcessingScheduler` and `BatchProcessingUseCase` instead of adding a parallel processing path.
- Add regression coverage for the recovery use case, repository query, and auto-configuration startup runner.

**Non-Goals:**
- Do not automatically retry `FAILED` or `STALLED` documents; they keep using manual retry.
- Do not change page-task worker recovery semantics.
- Do not change upload response contracts, Dashboard fields, or callback delivery.
- Do not introduce a distributed scheduler or cross-node lease in this change.

## File-Level Implementation Plan

### Core domain contracts

- `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentJobRepository.java`
  - Add `listQueuedBatchIds(int limit)` or an equivalent method that returns distinct batch IDs containing `DocumentStatus.QUEUED` documents.
  - Keep the method batch-oriented so the startup recovery service does not need to load every document row.

- `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/application/BatchStartupRecoveryService.java`
  - New application service.
  - Dependencies: `DocumentJobRepository`, `BatchProcessingScheduler`.
  - Method: `recoverQueuedBatches(int limit)`.
  - Behavior: query queued batch IDs, call `schedule(batchId)` once per batch, return the scheduled count, log recovery summary.

### Infrastructure repositories

- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentJobRepository.java`
  - Implement `listQueuedBatchIds(int limit)` using a distinct batch ID query filtered by `status = QUEUED`, ordered deterministically, limited by the configured recovery limit.

- `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseRepositories.java`
  - Extend the in-memory `DocumentJobRepository` test double with the new method.

- Other test doubles that implement `DocumentJobRepository`
  - Add the method where compilation requires it. Prefer a default interface implementation if it keeps test churn low and remains correct.

### Auto-configuration and runtime settings

- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensProcessingAutoConfiguration.java`
  - Register `BatchStartupRecoveryService`.
  - Register an `ApplicationRunner` that invokes startup recovery after the context is ready.
  - Keep startup recovery conditional on `autoProcessOnUpload=true`; when automatic processing is disabled, persisted queued batches should remain externally controlled.

- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringProperties.java`
  - Add a `batchStartupRecovery` property group if the existing properties structure supports it cleanly.
  - Proposed defaults: `enabled=true`, `limit=100`.
  - If adding properties would create large config churn, keep an internal default limit first and document the follow-up.

### Tests

- `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/ingestion/application/BatchStartupRecoveryServiceTest.java`
  - New RED test proving queued documents cause exactly one schedule call per owning batch.
  - Cover deduplication when one batch has multiple queued documents.
  - Cover limit behavior.
  - Cover no scheduling when no queued documents exist.

- `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentJobRepositoryTest.java`
  - Add repository test for `listQueuedBatchIds`.
  - Verify it returns distinct queued batch IDs and excludes completed/failed/stalled-only batches.

- `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensProcessingAutoConfigurationTest.java` or a nearby auto-configuration test
  - Add coverage that the startup runner invokes the recovery service when auto processing is enabled.
  - If no suitable test harness exists, add a narrow bean wiring test for the service and runner.

## Data Flow

1. Caller uploads a batch.
2. Upload transaction persists object-storage bytes, batch row, document rows, and initial events.
3. If the service crashes before or during asynchronous batch scheduling, those queued document rows remain in storage.
4. On application startup, the startup recovery runner scans for distinct batch IDs with queued documents.
5. Each recovered batch ID is passed to `BatchProcessingScheduler.schedule(batchId)`.
6. `BatchProcessingUseCase` processes only queued documents for that batch.
7. Existing page-task recovery continues the OCR phase for already-created page tasks.

## Risks / Trade-offs

- A startup scan can resubmit a batch that is also being processed by another node. The current system has no distributed batch lease, so this design relies on `BatchProcessingUseCase` only selecting queued documents and page-task claim locks preventing duplicate page OCR execution.
- A small limit may require multiple restarts to drain many queued batches unless a later scheduled scanner is added. The first implementation intentionally limits scope to startup recovery.
- Automatically retrying failed/stalled documents would hide real OCR or conversion failures, so that remains manual.

## Testing

- Run the focused startup recovery service test.
- Run the focused MyBatis repository test for queued batch lookup.
- Run the relevant auto-configuration test.
- Run the broader ingestion/processing slice that covers create batch, manual retry, batch processing, and page task worker recovery.
- Run `openspec validate 2026-06-20-batch-startup-recovery --strict`.
