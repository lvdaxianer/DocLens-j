## Context

The system now has several delivery-grade resilience mechanisms that are spread
across batch processing, page task execution, OCR routing, callback delivery,
LLM chunking, and startup recovery. The technical delivery document should make
those mechanisms easy to evaluate without forcing readers to reconstruct them
from individual implementation classes.

## Goals / Non-Goals

**Goals:**
- Explain "高并发、 高可用、 高性能" as an integrated delivery capability.
- Ground each claim in actual implementation mechanisms rather than fixed SLA
  numbers.
- Highlight technical balance: durable queues over synchronous OCR, capacity
  matching over blindly increasing threads, and idempotent recovery over
  optimistic best-effort execution.
- Keep the 100-document flow as the detailed operational flow and make the new
  section a higher-level evaluation lens.

**Non-Goals:**
- Do not modify runtime behavior or default values.
- Do not add synthetic throughput guarantees.
- Do not duplicate the full configuration reference.
- Do not replace the existing concurrency, load balancing, high availability,
  and tuning sections.

## Decisions

- Place `三高保障体系` after `总体架构` and before the end-to-end flow so readers
  first see the delivery capability model, then drill into the detailed
  processing path.
- Use a compact table for the three dimensions, followed by concise bullets for
  implementation-backed details and trade-offs.
- Explicitly mention startup recovery because it is a new high-availability
  capability: queued batches can be rediscovered after process restart and
  rescheduled when upload auto-processing is enabled.
- Keep wording careful: describe mechanisms and expected behavior, not
  absolute throughput or availability promises.

## Testing

- Run `openspec validate technical-delivery-three-highs --strict`.
- Use the documentation check script as the focused RED/GREEN verification by
  first adding required-content checks and confirming they fail before the
  document section exists.
- Re-run the documentation check after the document update.
- Run whitespace and placeholder scans over changed files.
