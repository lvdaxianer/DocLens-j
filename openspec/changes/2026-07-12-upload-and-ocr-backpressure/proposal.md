## Why

The upload API currently relies on Dashboard-side validation for the documented
single-batch file count limit. Frontend validation improves user experience, but
it is not a trust boundary for direct HTTP callers, SDK integrations, or
OpenWebUI-style clients.

The OCR dispatch path also contains an in-memory pending queue and a blocking
dispatch wait. Under OCR node saturation, unhealthy node recovery, or a burst of
page tasks, worker threads need a bounded waiting contract so the service can
fail recoverably instead of holding threads indefinitely.

## What Changes

- Enforce the documented 30-file upload limit on the server upload mapper.
- Tighten callback URL validation so only `http` and `https` URI schemes are
  accepted.
- Make OCR pending dispatch bounded by configured capacity and timeout.
- Return a stable dispatch failure when the pending queue is full or dispatch
  waiting times out, allowing the existing page-task failure path to persist the
  recoverable failure state.

## Impact

- Direct HTTP callers receive the same upload constraints as the Dashboard.
- Invalid callback schemes are rejected before batch creation.
- Saturated OCR routing has explicit backpressure behavior instead of unbounded
  memory growth or indefinite worker-thread retention.
- No database migration is required.
