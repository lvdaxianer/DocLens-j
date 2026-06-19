## Context

`DocumentRetryUseCase.retry()` currently reloads the document, validates that it is retryable, calls `document.retry(...)`, updates the document row, saves a retry event, refreshes the batch summary, and re-schedules the batch. It never clears the document's page-level child records.

The page task and page result tables both enforce uniqueness per `document_id` and `page_no`, so a second processing pass must start from a clean child state.

## Goals / Non-Goals

**Goals:**
- Clear existing page tasks for the target document before the document is scheduled again.
- Clear existing page OCR results for the target document before the document is scheduled again.
- Preserve current retry validation, event emission, batch summary refresh, and batch rescheduling.
- Add regression coverage that shows retry no longer collides with leftover child rows.

**Non-Goals:**
- Do not change the retryability rules for failed vs completed documents.
- Do not change page task generation, OCR processing, or merge behavior.
- Do not change the database schema or uniqueness constraints.

## Decisions

- Add explicit delete-by-document methods to the page task and page result repositories so retry can clear old child state in one transaction.
- Invoke child cleanup inside the retry transaction before the document is reset and re-queued.
- Keep the retry operation idempotent with respect to a single retry attempt: if the job restarts, it should rebuild the page rows from scratch instead of reusing the old children.

## Data Flow

1. Operator triggers document retry.
2. The use case loads the document and batch and validates retryability.
3. The use case deletes existing page results and page tasks for that document.
4. The use case resets the document status and records the retry event.
5. The batch summary is refreshed and the batch is re-scheduled.
6. The next page-preparation run can recreate page rows without hitting old unique-key collisions.

## Risks / Trade-offs

- Deleting child rows on retry removes any partially useful page-level history for that document, but that is the correct behavior for a fresh retry run.
- The cleanup must stay inside the retry transaction to avoid a race where the document is re-queued before its child rows are removed.

## Testing

- Add a failing unit test that shows retry must remove prior page tasks and page results before scheduling the batch again.
- Extend repository tests so the new delete-by-document methods actually remove all matching child rows.
- Run the focused retry and repository tests, then the broader backend slice that exercises document processing.
