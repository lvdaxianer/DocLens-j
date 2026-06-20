## Why

The page-task OCR path can process document pages concurrently and then merge
page results in `DocumentPageTaskAggregationService`. That aggregation path
currently saves OCR text directly. It does not invoke the configured LLM Markdown
post-processor, so documents processed by the faster page-task path can show
`LLM 未应用` even when healthy LLM configurations exist.

## What Changes

Attach the same Markdown post-processing flow used by the batch result builder
to the page-task aggregation path. After all page OCR tasks finish, the
aggregation service should merge OCR text, call the configured Markdown
post-processor, write the final Markdown text, and persist the same LLM
observability fields used by the normal result-building path.

## Impact

The OCR page task concurrency model stays unchanged. The change affects only the
final document aggregation step after all pages are completed. Saved results from
the page-task path will now include `llm_markdown_applied`, LLM chunk metadata
when present, warnings, and optional failure messages.
