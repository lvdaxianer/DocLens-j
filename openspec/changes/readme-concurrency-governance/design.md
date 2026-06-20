## Context

The current implementation uses several independent concurrency boundaries:
batch upload creates durable records, document processing uses a dedicated
executor, page OCR uses persisted page tasks plus a periodic worker, and OCR
routing uses node-level slots with a pending queue. The README should connect
these pieces without duplicating all low-level configuration details from
`docs/configuration.md`.

## Goals / Non-Goals

**Goals:**
- Explain what happens when many documents are uploaded at once.
- Explain which documents/pages execute immediately and which wait.
- Explain how OCR node selection, node capacity, pending dispatch, health
  governance, retry, and failover work at a high level.
- Explain how duplicate parsing and duplicate consumption are prevented.
- Mention that LLM Markdown chunking is a post-OCR stage with its own executor.

**Non-Goals:**
- Do not add or change any runtime behavior.
- Do not create a full configuration reference in README.
- Do not describe every adapter-specific HTTP payload.
- Do not modify English README in this change.

## Decisions

- Place the new section after `文档处理流水线` so readers first understand file
  type routing and then learn concurrency and governance.
- Use one Mermaid sequence diagram for the end-to-end path because it makes the
  upload, page task, OCR routing, and aggregation handoffs clear.
- Use short bullet lists and tables for limits and protection mechanisms so the
  README stays scannable.
- Reference actual implementation names where useful: `documentProcessingExecutor`,
  `ocr_document_page_tasks`, `PageTaskWorkerScheduler`, `OcrDispatchCoordinator`,
  `maxConcurrency`, `locked_until`, and unique `(document_id, page_no)` guards.

## Testing

- Run `openspec validate readme-concurrency-governance --strict`.
- Run the repository documentation check script if available.
- Inspect the README diff to ensure the added explanation matches the current
  implementation and contains no placeholder text.
