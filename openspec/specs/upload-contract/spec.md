# upload-contract Specification

## Purpose
Define server-side upload contract checks for batch size and callback URL
schemes.
## Requirements
### Requirement: Batch upload rejects oversized batch counts server-side

The server upload API MUST reject a batch that contains more than 30 uploaded
files before creating batch or document records.

#### Scenario: Direct multipart caller exceeds file count limit

- **GIVEN** a direct HTTP caller submits 31 multipart files
- **WHEN** the server maps the upload request
- **THEN** the request is rejected with a validation error
- **AND** no batch creation command is produced

### Requirement: Callback URL scheme is strictly validated

The server upload API MUST accept callback URLs only when the URI scheme is
`http` or `https`.

#### Scenario: Invalid callback scheme is rejected

- **GIVEN** an upload request includes `callback_url` with scheme `ftp`
- **WHEN** the server maps the upload request
- **THEN** the request is rejected with a validation error

#### Scenario: HTTPS callback URL is accepted

- **GIVEN** an upload request includes a well-formed `https` callback URL
- **WHEN** the server maps the upload request
- **THEN** the upload request mapping succeeds
