## Context

The synchronous OCR result builder reports `MERGE_TEXT` and `SAVE_TEXT` before
Markdown post-processing, so the dashboard can show that OCR has finished and LLM
formatting has begun. The page-task aggregation builder lacks the same progress
checkpoint. It gathers page OCR results, runs Markdown post-processing outside the
database transaction, and only updates the document when the final result is saved.

## Goals / Non-Goals

**Goals:**
- After the last page task completes, persist a visible transition out of
  `OCR_IMAGES` before any slow Markdown post-processing.
- Preserve short transaction boundaries; do not run the LLM call while holding a
  database transaction.
- Keep duplicate final-page callbacks idempotent and keep final completion behavior
  unchanged.

**Non-Goals:**
- Do not change page OCR worker concurrency.
- Do not change LLM chunking, retry, prompt, or fallback semantics.
- Do not change dashboard labels.

## Decision

Add explicit page-task aggregation progress checkpoints:

1. The final page success transaction keeps marking all pages complete.
2. Before building the final result, aggregation persists `MERGE_TEXT` with
   completed page count equal to total pages.
3. Immediately before Markdown post-processing, aggregation persists `SAVE_TEXT`
   with completed page count equal to total pages.
4. Markdown post-processing remains outside long database transactions.
5. Final save continues to persist the OCR result and mark the document completed.

## Data Flow

1. A page worker completes the last page OCR task.
2. Aggregation updates page progress to full count.
3. Aggregation persists `MERGE_TEXT` and then `SAVE_TEXT` in short transactions.
4. The LLM Markdown formatter runs while the document is visibly in the LLM/save
   stage.
5. The completed result is saved and the document reaches `COMPLETED`.

## Risks / Trade-offs

- `MERGE_TEXT` can be very brief because merging page text is in-memory and fast.
  The important correctness point is that `SAVE_TEXT` is persisted before the slow
  LLM call, so the UI no longer appears stuck in OCR.
- This introduces one additional progress update. It is acceptable because it is a
  short single-document update and prevents misleading user-facing status.

## Testing

- Add a regression test that blocks/observes Markdown post-processing and asserts
  the document has already reached `SAVE_TEXT` before the LLM processor returns.
- Keep the existing test that proves Markdown post-processing is outside the
  transaction.
- Run the focused aggregation test class and the relevant backend test slice.
