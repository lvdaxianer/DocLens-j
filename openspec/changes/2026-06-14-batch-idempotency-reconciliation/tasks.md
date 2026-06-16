## 1. Contract Coverage

- [x] 1.1 Add a failing contract test for `GET /api/v1/batches/by-idempotency-key/{idempotencyKey}` that asserts the
  reconciliation payload, `documents` array, and the 404 body for an unknown `idempotency_key`.
- [x] 1.2 Add focused unit tests for batch status mapping and document status/stage/error mapping across queued,
  processing, completed, and failed snapshots.

## 2. Endpoint Delivery

- [x] 2.1 Add `getBatchByIdempotencyKey(...)` to `OcrQueryController` and `OcrQueryService`.
- [x] 2.2 Implement the reconciliation response builder with stable snake_case fields and normalized document
  snapshots.
- [x] 2.3 Return a dedicated 404 body when no batch exists for the supplied `idempotency_key`.

## 3. Documentation and Validation

- [x] 3.1 Update the OpenWebUI/OCR contract documentation so the reconciliation lookup is documented alongside the
  existing query endpoints.
- [x] 3.2 Run the focused tests, broader backend verification, `openspec validate`, and the final diff check.

## 4. Pass-through Idempotency Semantics

- [x] 4.1 Accept duplicate `idempotency_key` uploads in the create-batch flow with RED/GREEN coverage.
- [x] 4.2 Remove database uniqueness for `ocr_batches.idempotency_key` in fresh schema and migration path.
- [x] 4.3 Make idempotency-key reconciliation lookup stable under duplicates by returning the latest matching batch.
- [x] 4.4 Update Dashboard/API/OpenWebUI docs and specs so `idempotency_key` is described as a third-party
  pass-through/correlation value, not DocLens duplicate protection.
- [ ] 4.5 Run focused tests, broader backend/dashboard verification, OpenSpec validation, and final diff check.
