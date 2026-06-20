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

### Requirement: Upload preflight limits file count

The dashboard upload form MUST block batch submission before making a network
request when more than 30 files are selected.

#### Scenario: Too many files are selected

- **GIVEN** the user selected 31 files
- **WHEN** the user clicks `上传并解析`
- **THEN** the dashboard shows a warning that one batch supports at most 30 files
- **AND** the upload submit event is not emitted
- **AND** no upload request is sent by the page

### Requirement: Upload preflight limits total size

The dashboard upload form MUST block batch submission before making a network
request when the selected files exceed 500 MB total.

#### Scenario: Selected files exceed total upload size

- **GIVEN** the selected files total more than 500 MB
- **WHEN** the user clicks `上传并解析`
- **THEN** the dashboard shows a warning that one batch supports at most 500 MB
- **AND** the upload submit event is not emitted
- **AND** no upload request is sent by the page

