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

- [ ] 4.1 Add a failing create-batch test proving two uploads with the same non-empty `idempotency_key` both create
  separate batches and preserve the received key.
- [ ] 4.2 Remove application-level duplicate `idempotency_key` rejection from the create-batch flow.
- [ ] 4.3 Remove database uniqueness for `ocr_batches.idempotency_key` in fresh schema and migration path.
- [ ] 4.4 Make idempotency-key reconciliation lookup stable under duplicates by returning the latest matching batch.
- [ ] 4.5 Update Dashboard/API/OpenWebUI docs and specs so `idempotency_key` is described as a third-party
  pass-through/correlation value, not DocLens duplicate protection.
- [ ] 4.6 Run focused tests, broader backend/dashboard verification, OpenSpec validation, and final diff check.
