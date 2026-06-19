## Context

The current page-task worker uses hard-coded values in `DocLensPageTaskWorkerAutoConfiguration`: batch size 8, lock 60 seconds, executor pool size 8, and queue capacity 200. Separately, `doclens.paddle-ocr.timeout-seconds` is configured as 600 seconds in the server application, and the default OCR request thread pool has a maximum of 4 threads.

Logs show page tasks failing with `status=QUEUED, lockedBy=null` when the worker tries to complete them. That state matches the recovery worker resetting an expired `PROCESSING` task before the OCR request returned.

## Goals / Non-Goals

**Goals:**
- Prevent normal long-running OCR requests from being recovered as stale page tasks.
- Let the page-task worker runtime be configured without code changes.
- Keep local worker concurrency aligned with configured OCR request concurrency so a 10-concurrency node can be used.
- Add focused tests for property binding and auto-configuration behavior.

**Non-Goals:**
- Do not change the OCR node routing or weighted load-balancing algorithm.
- Do not change database tables or migration files.
- Do not introduce heartbeat lock renewal in this change.

## Decisions

- Add `doclens.page-task-worker` properties for batch size, lock seconds, pool size, queue capacity, recovery limit, and interval.
- Default `lockSeconds` to `max(60, doclens.paddle-ocr.timeout-seconds + 30)` so normal timeout-length calls keep their lock until completion or failure.
- Default `poolSize` and `batchSize` to the maximum enabled bootstrap-node concurrency when available, falling back to the previous page-worker default.
- Default OCR request executor max/core size to the same derived page-task worker concurrency when no explicit OCR request thread-pool config is provided.

## Data Flow

1. Spring binds `doclens.page-task-worker` and existing OCR properties.
2. Page-task auto-configuration resolves effective worker settings.
3. The worker claims up to the effective batch size and sets `locked_until` using the effective lock seconds.
4. The page-task executor and OCR request executor have enough local capacity to feed the configured node concurrency.
5. Recovery only resets tasks whose lock exceeded the real OCR request window.

## Risks / Trade-offs

- Longer locks mean a crashed process may take longer to recover a page task. The trade-off is intentional because false recovery corrupts the active execution path.
- Higher default local concurrency can increase OCR node pressure. It is still bounded by node capacity and can be lowered through configuration.

## Testing

- Add property binding tests for `doclens.page-task-worker`.
- Add auto-configuration tests that verify effective lock seconds and pool sizing.
- Run the focused auto-configuration tests and the existing page-task execution/recovery/retry tests.
