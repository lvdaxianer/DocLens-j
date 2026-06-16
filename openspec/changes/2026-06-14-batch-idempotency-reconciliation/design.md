## Context

DocLens-j already stores the batch `idempotency_key` and already includes it in
the OCR completion callback body. The field must be treated as an external
system correlation value, not as a DocLens-owned uniqueness guarantee.

The current create-batch flow checks `BatchRepository.findByIdempotencyKey(...)`
before inserting and raises `DuplicateResourceException` when a prior batch has
the same value. The schema also declares `ocr_batches.idempotency_key` as
`UNIQUE`. Both layers must be relaxed so DocLens accepts repeated keys and lets
the callback receiver decide what to do with them.

## Goals / Non-Goals

**Goals:**
- Accept duplicate `idempotency_key` values on upload.
- Store the received key unchanged on every batch.
- Preserve the stored key unchanged in callback payloads.
- Remove fresh-install and migrated database uniqueness for `ocr_batches.idempotency_key`.
- Keep the existing idempotency-key lookup endpoint as a compatibility helper
  that returns the latest matching batch when duplicates exist.

**Non-Goals:**
- Do not implement DocLens-side duplicate upload suppression.
- Do not make third-party callback receivers idempotent.
- Do not add new persistence tables or background jobs.
- Do not mutate batch or document state during reconciliation reads.

## Decisions

- Remove the create-batch idempotency rejection entirely.
- Update `V1__doclens_ocr_schema.sql` for fresh databases and add a `V16`
  migration for existing databases.
- Keep `BatchRepository.findByIdempotencyKey(...)` for compatibility, but define
  it as latest-match lookup under duplicate keys.
- Add explicit ordering to the MyBatis query so duplicate-key lookup is stable.
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

- Existing callers that assumed `idempotency_key` was unique must switch to the
  returned `batch_id` or their own callback-side idempotency decision.
- Dropping an unnamed inline unique constraint is database-specific; the migration
  must work for the supported embedded test database and avoid breaking fresh installs.
- The lookup endpoint remains useful for diagnostics but is no longer a source of
  uniqueness truth.
