## Context

DocLens-j already has the technical building blocks for a credible delivery
story: DDD module boundaries, short upload transactions, persisted page tasks,
worker locks, OCR routing, weighted load balancing, health governance, retry,
recovery, callback delivery, dashboard observability, and SDK/HTTP deployment
modes. The new document should convert these facts into a polished technical
delivery narrative.

## Goals / Non-Goals

**Goals:**
- Create a dedicated Chinese technical delivery document.
- Emphasize engineering capability, high availability, and technical balance.
- Explain the end-to-end processing path with diagrams and implementation
  anchors.
- Describe how DocLens-j handles high-volume uploads, page-level OCR scheduling,
  OCR node load balancing, retry, failover, circuit breaking, idempotency, and
  duplicate-consumption prevention.
- Provide capacity planning and tuning guidance.
- Link the document from README and include it in documentation checks.

**Non-Goals:**
- Do not turn README into the full delivery document.
- Do not claim capabilities not represented by the current implementation.
- Do not introduce new runtime guarantees, SLAs, or benchmark numbers.
- Do not modify code or API behavior.

## Decisions

- Use `docs/technical-delivery.md` as the authoritative delivery document.
- Keep README concise by linking to the new document under documentation
  navigation and replacing overly detailed delivery content with a compact
  summary if needed.
- Update `scripts/check-docs.sh` so the delivery document is required and key
  headings are verified.
- Use Mermaid diagrams because they render in Markdown and are already suitable
  for architecture and sequence explanations.
- Phrase trade-offs explicitly: database page tasks over direct in-memory work,
  memory pending queue over distributed dispatch queue, page-level scheduling
  over whole-document scheduling, synchronous upload persistence over synchronous
  OCR waiting, and configurable local concurrency over hard-coded throughput
  promises.

## Testing

- Run `./scripts/check-docs.sh` before the document exists and confirm it fails
  for the missing required file or content.
- Add the document, README link, and docs check rule.
- Run `./scripts/check-docs.sh` and `openspec validate
  technical-delivery-document --strict`.
- Inspect the diff for accidental code changes and unsupported capability
  claims.
