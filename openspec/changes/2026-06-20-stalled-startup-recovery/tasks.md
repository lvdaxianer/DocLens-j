## 1. Recover stalled documents on startup

- [x] 1.1 Add failing core tests proving startup recovery resets `STALLED` documents to `QUEUED`, schedules their batch, and still skips `FAILED` documents.
- [x] 1.2 Extend repository contracts and MyBatis support with status-based distinct batch lookup.
- [x] 1.3 Implement startup stalled recovery with retry-style cleanup, state reset, summary refresh, event persistence, and single scheduling per batch.
- [x] 1.4 Update Spring auto-configuration wiring and tests for the expanded recovery service dependencies.
- [ ] 1.5 Run focused and broader recovery verification, strict code review, and archive the OpenSpec change.
