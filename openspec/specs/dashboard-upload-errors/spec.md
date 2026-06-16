# dashboard-upload-errors Specification

## Purpose
Define how the Dashboard upload page presents structured backend upload errors
and HTTP fallback messages.
## Requirements
### Requirement: Upload failures show backend detail
The Dashboard upload page MUST show the backend error detail when a batch upload
request fails with a structured error body containing `detail`.

#### Scenario: Backend detail is shown
- **WHEN** the upload API returns a non-2xx response
- **AND** the response body includes a non-empty `detail`
- **THEN** the upload error displayed to the user is that detail
- **AND** the UI does not replace it with only the HTTP status text

### Requirement: Upload failures keep HTTP fallback
The Dashboard upload page MUST keep a stable HTTP status fallback when a failed upload response does not include a usable `detail` field.

#### Scenario: Failed response has no detail
- **WHEN** the upload API returns a non-2xx response
- **AND** the response body is empty, malformed, or lacks a non-empty `detail`
- **THEN** the upload error displayed to the user includes the HTTP status and status text
