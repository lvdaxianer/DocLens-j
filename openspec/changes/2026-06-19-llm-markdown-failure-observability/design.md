## Context

The Markdown post-processing service already retries three times and then falls back
to OCR text. However, the fallback result only stores `RuntimeException.getMessage()`,
and the HTTP post-processor wraps all `IOException` and interrupted failures in a
generic `IllegalStateException("LLM Markdown request failed")`.

That means the persisted result loses the actual root cause.

## Goals / Non-Goals

**Goals:**
- Preserve the most specific root cause message available when Markdown post-processing fails.
- Keep the retry and fallback behavior unchanged.
- Make the result payload and logs useful for debugging timeout / transport / HTTP failures.

**Non-Goals:**
- Do not change the Markdown prompt.
- Do not change the OCR text that is sent to the LLM.
- Do not add automatic compatibility aliases for legacy `usage_type` values.

## Decisions

- Expose the deepest meaningful exception message in the fallback result.
- Keep the existing generic wrapper for user-facing HTTP errors only when no better
  message exists.
- Add focused tests around the fallback message so future regressions are caught.

## Data Flow

1. LLM request fails inside the HTTP Markdown post-processor.
2. The exception is wrapped with the most specific cause message preserved.
3. The Markdown post-processing service exhausts retries and stores that concrete message.
4. The OCR result API returns the preserved root cause string in `llm_error_message`.

## Risks / Trade-offs

- Exposing the precise failure message may reveal provider-specific error text, so
  we should keep the message focused on the transport/error class and avoid logging
  request bodies.
- The change does not fix provider outages; it only makes them diagnosable.

## Testing

- Add a failing unit test for the fallback message when the downstream processor throws.
- Add a focused test that the HTTP processor keeps the root cause message for
  wrapped IO failures.
- Run the relevant backend test slice and verify the new result message is surfaced.
