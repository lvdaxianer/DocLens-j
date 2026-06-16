# dashboard-upload-errors Specification

## Purpose
TBD - created by archiving change 2026-06-16-upload-error-detail. Update Purpose after archive.
## Requirements
### Requirement: Upload failures show backend detail
The Dashboard upload page MUST show the backend error detail when a batch upload request fails with a structured error body containing `detail`.

#### Scenario: Duplicate idempotency key is shown
- **WHEN** the upload API returns HTTP 409
- **AND** the response body includes `detail` with `duplicate idempotency key`
- **THEN** the upload error displayed to the user is `duplicate idempotency key`
- **AND** the UI does not replace it with only `409 Conflict`

### Requirement: Upload failures keep HTTP fallback
The Dashboard upload page MUST keep a stable HTTP status fallback when a failed upload response does not include a usable `detail` field.

#### Scenario: Failed response has no detail
- **WHEN** the upload API returns a non-2xx response
- **AND** the response body is empty, malformed, or lacks a non-empty `detail`
- **THEN** the upload error displayed to the user includes the HTTP status and status text

