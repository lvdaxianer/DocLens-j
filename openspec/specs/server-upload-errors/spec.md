# server-upload-errors Specification

## Purpose
TBD - created by archiving change 2026-06-20-markdown-table-upload-guard. Update Purpose after archive.
## Requirements
### Requirement: Multipart upload limit matches product cap

The server MUST configure multipart upload limits to allow dashboard batches up
to 500 MB.

#### Scenario: Application starts with upload limit config

- **WHEN** the server application context starts
- **THEN** `spring.servlet.multipart.max-file-size` is `500MB`
- **AND** `spring.servlet.multipart.max-request-size` is `500MB`

### Requirement: Oversized multipart requests return structured detail

The server MUST return HTTP 413 with a JSON `detail` field when multipart upload
size limits are exceeded.

#### Scenario: Multipart request exceeds configured limit

- **GIVEN** a multipart upload request exceeds the configured size limit
- **WHEN** the server maps the exception to an HTTP response
- **THEN** the response status is 413
- **AND** the response body contains a non-empty `detail` field suitable for
  dashboard display

