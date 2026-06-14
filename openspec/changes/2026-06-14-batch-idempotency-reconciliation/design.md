## Context

DocLens-j already stores the batch `idempotency_key` and already exposes batch/document/result queries.
What is missing is a single batch-level reconciliation endpoint that lets an external caller look up a batch
from the original `idempotency_key` it submitted at upload time.

## Goals / Non-Goals

**Goals:**
- Add a batch lookup endpoint by `idempotency_key`.
- Return one stable JSON snapshot that includes batch metadata and the document list.
- Preserve the existing heartbeat endpoint behavior.
- Keep the implementation in the existing query layer instead of adding a separate adapter stack.

**Non-Goals:**
- Do not change OCR ingestion, callback delivery, or task scheduling behavior.
- Do not add new persistence tables or background jobs.
- Do not mutate batch or document state during reconciliation reads.

## Decisions

- Extend the existing `OcrQueryController` and `OcrQueryService` instead of introducing a new controller tree.
- Reuse `BatchRepository.findByIdempotencyKey(...)` as the lookup source of truth.
- Build the response from the stored batch plus `DocumentJobRepository.listByBatchId(batch.batchId())`.
- Normalize reconciliation fields to the stable snake_case contract expected by the caller.
- Return a dedicated 404 body for missing reconciliation batches so callers can distinguish absence from transport failure.

## Response Mapping

- Batch `status` is returned as lower-case stable values: `queued`, `processing`, `completed`, `failed`.
- Document `status` is returned as lower-case stable values: `queued`, `processing`, `completed`, `failed`.
- Document `stage` is normalized to the stable reconciliation vocabulary so internal stages do not leak out.
- Missing `result_id` becomes an empty string.
- Missing `error.code` / `error.message` become empty strings.
- `documents` is always returned as an array, even for single-file batches.

## Risks / Trade-offs

- The reconciliation endpoint introduces a new response shape next to the existing generic query payloads.
- Stage normalization must stay aligned with the stored processing lifecycle; otherwise callers could see a misleading
  progress phase.
