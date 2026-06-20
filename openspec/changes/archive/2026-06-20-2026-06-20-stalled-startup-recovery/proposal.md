## Why

The first batch startup recovery implementation only resubmits batches that still contain
`QUEUED` documents. In production, stale detection can mark interrupted documents as
`STALLED` while their stage still shows `OCR_QUEUED`. After a service restart those
documents remain visible as "已卡住" and are not picked up by `BatchProcessingUseCase`,
because batch processing only selects `QUEUED` documents.

## What Changes

Extend startup recovery so that it also finds batches containing `STALLED` documents,
resets those stalled documents through the same retry semantics used by manual retry,
cleans stale page-level children, refreshes batch summaries, records a recovery event,
and then resubmits the owning batch.

## Impact

This change affects startup recovery only. It does not automatically retry ordinary
`FAILED` documents. Manual retry remains available for explicit business failures.
The startup recovery path becomes capable of continuing after crashes that already
left documents in the dashboard "已卡住" state.
