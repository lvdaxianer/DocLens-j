## Why

The root README explains the project purpose and document pipeline, but it does
not yet describe how DocLens controls high-volume uploads, page-level OCR
concurrency, OCR node load balancing, retries, circuit breaking, and duplicate
consumption prevention. Operators need that overview before tuning the runtime
or diagnosing a busy batch.

## What Changes

Add a README section that explains the concurrent processing flow from batch
upload through document workers, page tasks, OCR routing, result aggregation,
LLM Markdown chunking, retries, and recovery. Include a Mermaid sequence diagram
and concise mechanism tables for concurrency limits, load balancing, circuit
breaking, retry, and idempotency.

## Impact

This is documentation-only. It does not change processing behavior, runtime
configuration defaults, database schema, or public APIs.
