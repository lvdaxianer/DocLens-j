## Context

`GET /api/v1/dashboard/batches/{batchId}` already returns `callback_jobs`.
The Vue Dashboard currently renders the batch summary, OCR route, document
track, document table, and result drawer, but it does not show callback
outcomes. This makes callback failure diagnosis invisible on the page.
Operators also need to manually retry a failed callback from the same page when
the downstream receiver is restored.

## Goals / Non-Goals

**Goals:**
- Show callback delivery results on the existing batch detail page.
- Make failed callbacks obvious by displaying `failure_reason` and
  `failure_detail`.
- Show retry state with `retry_count` and `next_retry_at`.
- Provide a failed-callback retry action that immediately reuses the persisted
  callback payload and refreshes the page afterward.
- Preserve the existing route-level data orchestration in `BatchDetailView`.

**Non-Goals:**
- Do not add a separate callback detail route.
- Do not introduce a separate callback payload editor; manual retry reuses the
  persisted callback job payload.
- Do not bypass the existing failure reason contract; failed manual retries use
  the same recorded `failure_reason` and `failure_detail` fields.

## Decisions

- Add a focused `BatchCallbackJobsPanel` component under
  `doclens-dashboard/src/components/dashboard/`.
- Keep `BatchDetailView` as the composition surface and pass
  `selectedBatch.callback_jobs` into the new component.
- Add a typed `CallbackJobRow` interface to the Dashboard response model.
- Add a Dashboard POST endpoint for manual callback retry:
  `POST /api/v1/dashboard/callback-jobs/{callbackJobId}/retry`.
- Expose a `CallbackDeliveryWorker.retryNow(callbackJobId)` method that loads
  the existing job by ID and sends it through `CallbackDeliveryProcessor`.
- Use the existing compact panel visual language so operators can scan callback
  outcomes without leaving the batch detail page.
- Render an empty state when a batch has no callback jobs.
- Only failed callback jobs show an enabled retry action. Retry loading is
  scoped to a single callback job ID, and the parent route refreshes batch
  detail after the POST resolves.

## Component Map

- `BatchDetailView`: owns route/store refresh, passes callback jobs as a prop.
- `DashboardController`: exposes the manual retry endpoint for Dashboard use.
- `CallbackDeliveryWorker`: owns immediate retry by callback job ID while
  reusing the scheduled delivery processor.
- `BatchCallbackJobsPanel`: displays callback jobs, status, retry count,
  next retry time, failure reason/detail, and failed-job retry action.
- `StatusTag`: remains unchanged for batch/document status; callback status
  receives local labels in the callback panel to avoid changing global status
  semantics.

## Testing

- Add a focused UI test that mounts `BatchDetailView` with `callback_jobs` in
  the mocked batch detail response and asserts the callback section shows the
  failed status and failure detail.
- Add focused backend tests for `CallbackDeliveryWorker.retryNow(...)` success
  and retry failure reason recording.
- Add a focused UI test that clicks the failed callback retry action and
  asserts the Dashboard retry API wrapper is called and batch detail is
  refreshed.
- Run Dashboard UI tests and the frontend build after implementation.
