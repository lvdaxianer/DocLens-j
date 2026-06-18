## Why

The current Markdown post-processing path collapses every upstream LLM failure into
`LLM Markdown request failed`, which makes it impossible to tell whether the real
problem was a timeout, a transport issue, an HTTP error response, or a response
parse failure.

## What Changes

Preserve the underlying Markdown post-processing failure reason when the LLM call
fails, so the saved OCR result and UI can show a concrete cause instead of a generic
message.

## Impact

This only changes the failure detail that is surfaced for LLM Markdown fallback.
It does not change the Markdown prompt, the OCR text sent to the model, or the
success path.
