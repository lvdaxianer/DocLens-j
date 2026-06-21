## Context

Runtime evidence shows a completed batch whose OCR node calls exist in the node
management API:

- `/api/v1/ocr-models/paddle_ocr/nodes` reports the runtime node and two
  successful images today.
- `/api/v1/ocr-nodes/paddle_ocr-10-100-30-215-8080/calls` lists the two
  successful calls for the document.
- `/api/v1/dashboard/batches/{batchId}` returns `batch_dispatch_hit_nodes=[]`,
  document `ocr_final_hit_nodes=[]`, and `ocr_runtime_capacity.nodes=[]`.
- `/api/v1/dashboard/ocr-health` returns `ocr_resources.nodes=[]`.

The Dashboard empty values match `EmptyDashboardOcrMetricsProvider`, while node
management uses the real repositories. This means the bug is in Spring bean
wiring rather than OCR execution.

## Goals / Non-Goals

**Goals:**
- Make the real Dashboard OCR metrics provider win whenever its required OCR
  infrastructure beans exist.
- Keep the empty provider as an embedded-host fallback only.
- Add regression coverage proving the auto-configured `DashboardQueryService`
  uses the real provider and exposes OCR resource nodes.
- Preserve existing Dashboard API field names and response shape.

**Non-Goals:**
- Do not change OCR routing, dispatch selection, or node health state.
- Do not change OCR call persistence schema.
- Do not add debug-only endpoints.
- Do not mask missing backend data with front-end placeholder text.

## Decisions

- Treat the empty provider as a fallback bean that must be created only after
  Spring has had a chance to create the real provider.
- Add auto-configuration coverage around the whole wiring path instead of only
  unit testing `OcrDashboardMetricsProvider` directly.
- Verify with runtime evidence after rebuilding and restarting the local dev
  server: Dashboard OCR health and batch detail should expose OCR nodes and the
  completed document's final OCR node assignment.

## Testing

- Add a focused auto-configuration regression test that loads DocLens runtime
  configuration with OCR repositories and asserts `DashboardQueryService`
  receives a non-empty OCR resource response from the real provider.
- Run the focused auto-configuration test.
- Run the relevant starter/query test slice.
- Rebuild/restart the dev server and verify the Dashboard endpoints against the
  existing batch.
