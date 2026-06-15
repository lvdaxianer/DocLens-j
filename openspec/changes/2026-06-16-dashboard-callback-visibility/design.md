## Context

`GET /api/v1/dashboard/batches/{batchId}` already returns `callback_jobs`.
The Vue Dashboard currently renders the batch summary, OCR route, document
track, document table, and result drawer, but it does not show callback
outcomes. This makes callback failure diagnosis invisible on the page.

## Goals / Non-Goals

**Goals:**
- Show callback delivery results on the existing batch detail page.
- Make failed callbacks obvious by displaying `failure_reason` and
  `failure_detail`.
- Show retry state with `retry_count` and `next_retry_at`.
- Preserve the existing route-level data orchestration in `BatchDetailView`.

**Non-Goals:**
- Do not add manual callback retry actions.
- Do not change backend callback delivery behavior.
- Do not add a separate callback detail route.

## Decisions

- Add a focused `BatchCallbackJobsPanel` component under
  `doclens-dashboard/src/components/dashboard/`.
- Keep `BatchDetailView` as the composition surface and pass
  `selectedBatch.callback_jobs` into the new component.
- Add a typed `CallbackJobRow` interface to the Dashboard response model.
- Use the existing compact panel visual language so operators can scan callback
  outcomes without leaving the batch detail page.
- Render an empty state when a batch has no callback jobs.

## Component Map

- `BatchDetailView`: owns route/store refresh, passes callback jobs as a prop.
- `BatchCallbackJobsPanel`: displays callback jobs, status, retry count,
  next retry time, and failure reason/detail.
- `StatusTag`: remains unchanged for batch/document status; callback status
  receives local labels in the callback panel to avoid changing global status
  semantics.

## Testing

- Add a focused UI test that mounts `BatchDetailView` with `callback_jobs` in
  the mocked batch detail response and asserts the callback section shows the
  failed status and failure detail.
- Run Dashboard UI tests and the frontend build after implementation.
