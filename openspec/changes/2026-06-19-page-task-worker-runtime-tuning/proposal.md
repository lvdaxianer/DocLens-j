## Why

Long-running page OCR requests can legitimately take longer than the current hard-coded 60 second page-task lock. When that happens, the recovery worker can reset a still-running page task back to `QUEUED`, and the original worker later fails to mark the page as completed because it no longer owns a `PROCESSING` row.

The configured OCR node also allows higher concurrency than the hard-coded page-task worker and OCR request pools. This makes an operator-visible `max-concurrency: 10` setting look ineffective because the local runtime can still bottleneck below that value.

## What Changes

Make page-task runtime settings configurable and align the defaults with OCR request behavior:

- Page-task lock duration defaults to at least the configured PaddleOCR request timeout.
- Page-task worker batch size and executor pool size become configurable.
- OCR request executor defaults can be sized from the same runtime concurrency so node `max-concurrency` is not silently capped by local thread pools.

## Impact

This changes local scheduling defaults and configuration binding only. It does not change OCR routing selection, page task schema, document retry semantics, or result aggregation behavior.
