## Why

When page-task OCR finishes all pages, the aggregation path calls Markdown LLM
post-processing before it persists any merge or save stage. If the LLM call is
slow, the dashboard continues to show `OCR 图片解析中` even though every page has
already completed.

## What Changes

Persist visible document progress after the final page OCR result arrives and
before slow post-processing starts. The page-task aggregation path should first
move the document into text merge progress, then move it into save/LLM formatting
progress before invoking Markdown post-processing.

## Impact

This is a status-flow fix only. It does not change page OCR concurrency, Markdown
prompting, LLM fallback behavior, final result persistence, or callback delivery.
