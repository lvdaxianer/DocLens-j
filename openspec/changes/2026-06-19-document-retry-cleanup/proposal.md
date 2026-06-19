## Why

Manual document retry currently only resets the document job itself. The old page tasks and page OCR results remain in place, so the next retry run can collide with the existing `document_id + page_no` unique constraints and fail immediately.

## What Changes

Before re-queuing a document for retry, clear that document's existing page tasks and page OCR results, then reset and reschedule the document as a fresh run.

## Impact

This changes retry semantics only. It does not change normal ingestion, page execution, OCR parsing, or the dashboard display contract. The change only affects how a failed or stalled document is re-entered into processing.
