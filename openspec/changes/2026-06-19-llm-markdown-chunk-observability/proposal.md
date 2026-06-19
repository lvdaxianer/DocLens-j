## Why

The chunked Markdown path already splits large documents into concurrent chunk
jobs, but the dashboard does not surface how many chunks were created or whether
that chunk executor is busy. As a result, users only see the generic `LLM 排版中`
state and may assume the system is processing one long task serially.

## What Changes

Expose chunk observability for Markdown post-processing by surfacing the chunk
count on batch detail document rows and by showing the LLM Markdown chunk executor
state in the OCR resource metrics area.

## Impact

This does not change chunk planning, chunk routing, or the Markdown prompt. It
only improves visibility into how many chunks were planned and whether the shared
chunk executor is active or backed up.
