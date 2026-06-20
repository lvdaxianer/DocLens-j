## 1. Add batch startup recovery

- [x] 1.1 Add failing tests for startup recovery service behavior: distinct queued batch resubmission, no completed/failed/stalled-only resubmission, and recovery limit handling.
- [ ] 1.2 Extend `DocumentJobRepository` and its in-memory test doubles with a distinct queued-batch lookup used by startup recovery.
- [ ] 1.3 Implement `BatchStartupRecoveryService` and wire it to `BatchProcessingScheduler`.
- [ ] 1.4 Add MyBatis repository support and tests for distinct queued batch ID lookup.
- [ ] 1.5 Add Spring auto-configuration startup runner and tests, enabled only when automatic upload processing is enabled.
- [ ] 1.6 Re-run the focused recovery/repository/auto-configuration tests and the broader ingestion/processing recovery test slice.
