## Why

DocLens already persists uploaded files, batches, document jobs, page tasks, and page results before asynchronous OCR work completes. Page-level OCR recovery also resets expired page locks after a restart. The missing gap is batch-level restart recovery: if the service crashes after the upload transaction commits but before the batch processing task creates page tasks, the batch can remain queued until a manual retry or another external trigger.

## What Changes

Add a startup recovery flow that scans persisted documents for queued work, deduplicates the owning batch IDs, and re-submits those batches through the existing `BatchProcessingScheduler`.

## Impact

The change affects service startup behavior only. Normal upload scheduling remains unchanged. Page-task recovery continues to handle already-created page OCR tasks. Failed and stalled documents are not automatically retried by this change; they continue to require the existing manual retry flow.
