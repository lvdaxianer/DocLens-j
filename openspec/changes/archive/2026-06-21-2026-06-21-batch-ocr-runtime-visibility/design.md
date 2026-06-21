## Context

`BatchOcrRoutePanel` receives `currentDocumentFinalHitNodes` and `hitNodes`. The former is derived only from successful `OcrNodeCall` rows, so it is empty during OCR. The latter combines completed call rows with `OcrBatchHitTracker` runtime snapshot, but it does not include node capacity or model aggregate capacity.

`DashboardQueryService.batchDetailView()` already has access to `DashboardOcrMetricsProvider`, which can read the runtime node pool and configured node rows through `ocrResources()`. The batch detail response can include a scoped runtime snapshot without changing dispatch mechanics.

LLM configs intentionally store a credential environment variable name and resolve it through `EnvironmentCredentialResolver`. The screenshot error is correct at runtime, but the UI needs to explain it in operator language and not imply that entering the variable name in the page creates the process environment variable.

## Goals / Non-Goals

**Goals:**
- Batch detail shows model-level OCR runtime capacity: current inflight images and max concurrency.
- Batch detail shows node-level OCR runtime capacity for the selected or hit OCR model/nodes.
- Current document allocation is visible while OCR is running, separately from final successful attribution.
- LLM config UI and result drawer explain missing environment variables in Chinese with actionable restart/setup guidance.

**Non-Goals:**
- Do not increase OCR worker concurrency or change routing/failover.
- Do not persist in-flight allocations to the database in this change.
- Do not store raw API keys in the database.
- Do not add a secret manager.

## Decisions

- Add batch detail fields derived from existing runtime metrics:
  - `ocr_runtime_capacity`: model/node capacity snapshot relevant to the batch route.
  - `ocr_running_hit_nodes_by_document`: current in-memory OCR allocation grouped by document when available.
- Preserve existing `batch_dispatch_hit_nodes` and `ocr_final_hit_nodes`; rename only UI labels to clarify final vs running semantics.
- Extend `BatchOcrHitNode` rows with optional runtime fields (`inflight_images`, `max_concurrency`, `running_image_count`) so older API shapes remain compatible.
- Keep credential behavior environment-variable based. Improve form hint, footer wording, validation copy, and result drawer error formatting.

## Testing

- Backend RED tests for batch detail runtime capacity and current-document running allocation.
- Frontend RED tests for rendering batch concurrency snapshot and running/final allocation sections.
- Frontend RED tests for LLM env-var copy and result drawer friendly missing-env message.
- Run focused backend/frontend tests, then relevant broader dashboard and OpenSpec validations.
