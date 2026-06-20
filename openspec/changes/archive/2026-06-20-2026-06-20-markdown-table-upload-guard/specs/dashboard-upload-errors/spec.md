# dashboard-upload-errors Specification

## Purpose
Define dashboard-side upload preflight validation before a batch upload request
is sent.

## ADDED Requirements
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
