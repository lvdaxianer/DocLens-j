## Why

The technical delivery document already explains the 100-document processing
flow, concurrency boundaries, load balancing, recovery, and trade-offs. After
the latest startup recovery logic was added, the document still lacks a
dedicated "three highs" summary that helps delivery reviewers quickly see how
DocLens-j balances high concurrency, high availability, and high performance.

## What Changes

Add a dedicated `三高保障体系` section to `docs/technical-delivery.md`. The
section will summarize the implementation-backed mechanisms for high
concurrency, high availability, and high performance, including layered
queueing, persisted page tasks, OCR node slot routing, startup recovery,
expired-lock recovery, idempotent constraints, weighted-idle routing, and
resource isolation.

Update the documentation verification script so the new section is protected
by required-content checks.

## Impact

This is documentation-only. It does not change runtime behavior,
configuration defaults, schemas, APIs, or processing semantics.
