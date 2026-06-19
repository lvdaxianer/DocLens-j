## Context

The upload pipeline already carries chunking and OCR routing options from the dashboard to the backend. Markdown post-processing is still invoked unconditionally after text extraction, even when an upstream system has already performed an LLM orchestration step and only wants DocLens to preserve the incoming text as-is.

## Goals / Non-Goals

**Goals:**
- Let the caller declare that a document was already LLM-orchestrated upstream.
- Preserve the current Markdown post-processing path for the default case.
- Avoid repeating LLM Markdown work when the flag is enabled.

**Non-Goals:**
- Do not add custom orchestration stages or a multi-state pipeline enum.
- Do not change the Markdown post-processor contract for the non-skip path.
- Do not alter chunk strategy behavior in this change.

## Decisions

- Use a boolean upload field named `llmOrchestrated` to represent the skip intent.
- Default the field to `false` so existing uploads keep their current behavior.
- Thread the flag through the batch request and document job model so the post-processing service can make the skip decision from document state.
- When the flag is `true`, return the extracted OCR text without invoking Markdown post-processing again.

## Data Flow

1. The dashboard upload form includes `llmOrchestrated` in the multipart request.
2. The backend request mapper parses the flag and stores it on the batch creation request.
3. The document job carries the flag into processing.
4. `DocumentOcrResultBuilder` or the Markdown post-processing service checks the flag before calling the LLM post-processor.
5. If the flag is enabled, DocLens preserves the extracted text and records that Markdown was not applied.

## Risks / Trade-offs

- A skipped post-processing step means the output text is trusted from upstream, so callers must only enable the flag when they really own that upstream normalization.
- Because the flag suppresses work, logs and result metadata still need to make the skip observable for debugging.

## Testing

- Add a failing backend test that proves an upload with `llmOrchestrated=true` bypasses Markdown post-processing.
- Add a failing backend test that proves the default `false` path still invokes Markdown post-processing.
- Add request-mapping coverage so the multipart flag reaches the batch creation request and document job.
