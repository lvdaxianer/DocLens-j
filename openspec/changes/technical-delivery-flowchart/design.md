## Context

The user wants the delivery document to communicate flow, not participant
message order. The clearest artifact is a Mermaid flowchart that uses one
high-volume scenario and shows every major queue and decision point through
completion.

## Goals / Non-Goals

**Goals:**
- Use a flowchart instead of a sequence diagram for the end-to-end path.
- Center the diagram on 100 simultaneously uploaded documents.
- Show document-processing executor behavior with core/max 6 and the remaining
  94 documents waiting in the executor queue.
- Show how worker completion pulls the next waiting document.
- Show page task persistence, page worker recovery/claiming, OCR node slot
  waiting, retry/failover/circuit handling, aggregation, optional LLM chunk
  processing, callback creation, and final batch state refresh.

**Non-Goals:**
- Do not introduce new throughput or SLA claims.
- Do not change runtime scheduling behavior.
- Do not rewrite unrelated sections of the delivery document.

## Decisions

- Keep the existing section heading `端到端处理链路` so README links and document
  structure remain stable.
- Replace only the Mermaid `sequenceDiagram` block and adjacent example text.
- Add doc-check keywords for `100 个文档` and `flowchart TD` so the flowchart
  remains part of the required delivery-document contract.
- Preserve the technical boundary that document-processing concurrency only
  covers document preparation/direct text handling; page OCR continues through
  the database-backed page-task worker and OCR slot governance.

## Testing

- Update `scripts/check-docs.sh` first and run it before the document change to
  confirm it fails on the missing `100 个文档`/`flowchart TD` requirement.
- Update `docs/technical-delivery.md`.
- Run `./scripts/check-docs.sh` and `openspec validate
  technical-delivery-flowchart --strict`.
- Inspect the diff for unsupported runtime claims or accidental code changes.
