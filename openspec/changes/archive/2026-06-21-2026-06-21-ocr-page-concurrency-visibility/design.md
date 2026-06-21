## Context

The current Dashboard receives `ocr_running_hit_nodes` and
`ocr_final_hit_nodes` on each document row. The running data comes from
`OcrBatchHitTracker`, whose runtime snapshot only stores document, model, node,
and image count. It intentionally aggregates by node and cannot answer which
page or thread is active.

Page-level OCR is executed by `DocumentPageTaskExecutionService`. That service
already knows the claimed `DocumentPageTask`, the configured `workerId`, and the
current Java thread at the moment it calls the routing service. This is the
right place to record live execution detail without changing OCR adapters.

## Decisions

- Add a separate runtime tracker for live OCR page tasks instead of expanding the
  node aggregate hit tracker.
- Record the live page task immediately before the OCR routing call and remove
  it in a `finally` block after success or failure.
- Store only runtime-safe identifiers and timestamps: batch ID, document ID,
  page number, model key/node ID after routing is selected, worker ID, thread
  name, and started time.
- Update the live entry with selected model/node after the routing service
  returns selection metadata where available.
- Expose live rows from the Dashboard batch detail read model as
  `ocr_running_page_tasks`.
- Render the rows in the existing `BatchOcrRoutePanel` so concurrency evidence
  sits next to routing allocation summaries.

## Data Flow

1. A page worker claims a queued page task.
2. The executor submits the claimed task to the page-task worker executor.
3. The worker thread records a live page task with page number, worker ID,
   thread name, and start time.
4. OCR routing selects a node and processes the image.
5. The live tracker updates or records the selected node/model for the page.
6. Batch detail reads the live tracker by batch ID and joins node display names.
7. Dashboard renders one row per in-flight image page.
8. On success or failure, the executor removes the live row.

## Error Handling

- If live tracking fails, OCR processing must continue. Observability must not
  block document processing.
- If a node name cannot be found, the Dashboard falls back to the node ID.
- If an OCR call completes between refreshes, the row disappears and the final
  allocation summary remains the source for completed work.

## Testing

- Add backend unit tests for the runtime tracker snapshot and cleanup behavior.
- Add backend batch detail assembly tests proving live page tasks are returned
  with page number, worker ID, thread name, node, model, and running duration.
- Add a frontend component test proving the OCR route panel renders multiple
  simultaneous page rows with distinct thread names.
- Run focused backend tests, focused frontend tests, Dashboard build, OpenSpec
  validation, diff check, and final audit.
