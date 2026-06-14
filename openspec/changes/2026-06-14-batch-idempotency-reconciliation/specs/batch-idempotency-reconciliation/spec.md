## ADDED Requirements

### Requirement: Batch reconciliation by idempotency key
DocLens-j MUST expose a batch reconciliation endpoint that allows callers to look up a batch by the same
`idempotency_key` they used when creating the batch.

#### Scenario: Reconciliation request succeeds
- **WHEN** a caller sends `GET /api/v1/batches/by-idempotency-key/{idempotencyKey}`
- **AND** a batch exists for that `idempotencyKey`
- **THEN** the API returns HTTP 200
- **AND** the response includes `batch_id`
- **AND** the response includes `idempotency_key`
- **AND** the response includes `status`
- **AND** the response includes `total_files`
- **AND** the response includes `completed_files`
- **AND** the response includes `failed_files`
- **AND** the response includes `progress_percent`
- **AND** the response includes a `documents` array
- **AND** each document includes `document_id`
- **AND** each document includes `file_name`
- **AND** each document includes `status`
- **AND** each document includes `stage`
- **AND** each document includes `progress_percent`
- **AND** each document includes `current_page`
- **AND** each document includes `total_pages`
- **AND** each document includes `result_id`
- **AND** each document includes `error.code`
- **AND** each document includes `error.message`
- **AND** each document includes `created_at`
- **AND** each document includes `updated_at`

#### Scenario: Reconciliation batch is missing
- **WHEN** a caller sends `GET /api/v1/batches/by-idempotency-key/{idempotencyKey}`
- **AND** no batch exists for that `idempotencyKey`
- **THEN** the API returns HTTP 404
- **AND** the response body includes code `404`
- **AND** the response body includes message `batch not found`

### Requirement: Reconciliation documents preserve stored ordering
DocLens-j MUST return documents in the same stored batch order so callers can safely align the response with the
original upload order.

#### Scenario: Single-file batch still returns a documents array
- **WHEN** a caller reconciles a batch that only contains one document
- **THEN** the response still includes `documents` as an array
- **AND** that array contains exactly one document snapshot
