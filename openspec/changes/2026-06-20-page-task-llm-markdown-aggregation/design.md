## Context

DocLens now has two ways to produce a final OCR result:

- The batch result builder path extracts text and then calls
  `DocumentMarkdownPostProcessingService`.
- The page-task path dispatches individual pages concurrently, stores page
  results, and then `DocumentPageTaskAggregationService` merges the final result.

The second path is the one visible in the user's recent run: the document pages
were OCRed concurrently, but the final result had no `llm_markdown_applied`
field in `rawVendorOutput`. The dashboard therefore correctly rendered `LLM 未应用`
for that result, even though the LLM configuration endpoint reported healthy and
enabled configs.

## Goals / Non-Goals

**Goals:**
- Keep page-level OCR concurrency and locking unchanged.
- Run configured LLM Markdown post-processing after page aggregation.
- Persist the same result metadata shape as the batch result builder:
  `ocr_text`, `llm_markdown_applied`, LLM metadata such as chunk count, warnings,
  and optional `llm_error_message`.
- Preserve current fallback behavior: if LLM post-processing fails after its
  retries, save OCR text and expose a clear fallback warning/error.

**Non-Goals:**
- Do not change LLM provider configuration APIs.
- Do not change page task claim/lock semantics.
- Do not change dashboard display rules in this change.

## Design

Add `MarkdownPostProcessor` to `DocumentPageTaskAggregationDependencies`.
`DocumentPageTaskAggregationService` will construct a
`DocumentMarkdownPostProcessingService` using that processor, similar to
`DocumentOcrResultBuilder`.

When all page tasks are completed:

1. Build a `DocumentTextExtractionResult` from persisted page results.
2. Call `DocumentMarkdownPostProcessingService.process(document, extracted)`.
3. Write `postProcessed.finalText()` to object storage.
4. Save an `OcrResult` using the post-processed final text, merged warnings, and
   raw output fields:
   - page raw outputs
   - original OCR text
   - `llm_markdown_applied`
   - any post-processor metadata, including chunk observability
   - optional `llm_error_message`

## Risks / Trade-offs

- Page-task completion will now wait for LLM Markdown post-processing before the
  document becomes completed. That is expected because the saved result should
  represent the final formatted output.
- If LLM is slow, the OCR part may finish quickly while final completion waits
  on LLM. The existing LLM chunk executor and per-config concurrency controls
  still govern throughput.
- Existing historical results without `llm_markdown_applied` remain unchanged
  unless retried or reprocessed.

## Testing

- Add a focused failing unit test proving page-task aggregation applies a
  configured Markdown post-processor and stores `llm_markdown_applied=true`.
- Add a focused failing unit test proving post-processor metadata such as
  `llm_chunk_count` is carried into the final raw output.
- Add a focused failing unit test proving LLM failure in page-task aggregation
  falls back to OCR text with the existing failure warning and error message.
- Re-run the focused page-task aggregation tests and relevant processing tests.
