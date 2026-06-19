## Why

Large Markdown documents are currently processed chunk by chunk in sequence,
which leaves the available LLM throughput underused even when multiple healthy
LLM configurations and per-config concurrency are available.

## What Changes

Parallelize Markdown chunk execution across a shared worker pool so chunks from
one document and chunks from multiple documents can be consumed concurrently,
while still selecting LLM configurations in round-robin order and honoring each
configuration's own concurrency and request-interval limits.

## Impact

This changes only the Markdown post-processing execution model. The OCR parsing
path, the batch lifecycle, and the saved result contract remain unchanged except
for the new chunk-parallel behavior and chunk-level fallback semantics.
