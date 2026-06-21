## Context

DocLens-j currently has multiple documentation entry points:

- `README.md` for Chinese readers;
- `README-en.md` for English readers;
- `docs/technical-delivery.md` for architecture, concurrency, high availability,
  and operational trade-offs;
- focused references under `docs/` for API, SDK, configuration, development,
  packaging, quick trial, and integrations.

Recent implementation work added or changed several user-visible concepts that
the top-level documentation must explain accurately: page-task based OCR
scheduling, total OCR concurrency derived from configured OCR nodes, global load
balancing as the preferred routing mode, startup recovery, page result
deduplication, chunk checkpoint persistence, LLM configuration precedence,
upload count and size limits, and dashboard concurrency visibility.

## Goals / Non-Goals

**Goals:**

- Make `README.md` a concise Chinese onboarding page that reflects current code.
- Make `README-en.md` an equivalent English onboarding page.
- Make `docs/technical-delivery.md` the durable technical reference for system
  positioning, architecture, processing flow, concurrency, crash recovery,
  checkpoints, observability, and tuning.
- Keep references to detailed API/configuration/SDK documents discoverable
  without duplicating every endpoint or property.
- Remove the stale uppercase English README path from the repository.

**Non-Goals:**

- Do not rewrite the entire `docs/` directory.
- Do not change runtime behavior, tests, API payloads, build scripts, or
  frontend code.
- Do not introduce new documentation tooling.

## Documentation Structure

`README.md` should answer:

- What is DocLens-j?
- Which deployment modes are supported?
- Which file types and processing stages are supported?
- How do I start the service locally?
- Where is the dashboard?
- Which current capabilities matter most: batch upload, OCR routing, global load
  balancing, restart recovery, LLM Markdown, upload guardrails, and observability?
- Where do I find the detailed API, SDK, configuration, development, packaging,
  integration, and technical delivery documents?

`README-en.md` should mirror the same information in English. It does not need
to be word-for-word identical, but it must preserve the same facts and links.

`docs/technical-delivery.md` should go deeper:

- module architecture and deployment shapes;
- end-to-end processing flow;
- concurrency layers and capacity allocation;
- OCR node routing and global load balancing;
- restart recovery and duplicate-consumption protection;
- LLM Markdown chunk checkpointing;
- upload guardrails and failure semantics;
- dashboard observability and operational tuning;
- technical trade-offs and future evolution.

## Validation

- Validate the OpenSpec change before documentation edits.
- Run the repository documentation check script if available.
- Inspect the final diff for stale names, broken links, placeholder text, and
  mismatch between Chinese and English README facts.
