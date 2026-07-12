## Architecture

This change keeps the current module boundaries. Upload validation stays in
`doclens-server` because it validates HTTP multipart request shape before
constructing the SDK request. OCR dispatch backpressure stays split between
`doclens-core` contracts and `doclens-spring-boot-starter` infrastructure:
core defines queue semantics and wait errors, while starter provides the
in-memory bounded implementation and configuration binding.

## Upload Contract

`CreateBatchRequestMapper` will enforce a server-side `MAX_UPLOAD_FILE_COUNT`
constant matching the Dashboard's 30-file rule. The mapper will also parse
callback URLs with `URI` and accept only `http` or `https`. Rejected inputs keep
the existing validation exception path so `GlobalExceptionHandler` continues to
return the standard validation response shape.

## OCR Dispatch Backpressure

`OcrPendingRequestQueue.enqueue` will report whether the request was accepted.
The in-memory queue will use a configured bounded capacity. When capacity is
exhausted, dispatch acquisition returns a failed result with a stable error
message. `OcrDispatchAcquireResult.awaitDispatch` will use a configured timeout
instead of unbounded `join()`.

The timeout belongs to `OcrRoutingServiceProperties` because it is part of OCR
routing behavior and can be derived from application configuration. Existing
page-task execution already catches routing exceptions and marks the page task
failed with a recoverable failure record, so this change does not add a new
database status.

## Testing

TDD tests will cover the upload server boundary before implementation, then the
OCR queue contract and routing wait behavior. Focused tests will run for the
touched server contract and OCR routing/auto-configuration slices, followed by
broader Maven verification for the affected modules.
