## MODIFIED Requirements

### Requirement: Upload failures show backend detail
The Dashboard upload page MUST show the backend error detail when a batch upload
request fails with a structured error body containing `detail`.

#### Scenario: Backend detail is shown
- **WHEN** the upload API returns a non-2xx response
- **AND** the response body includes a non-empty `detail`
- **THEN** the upload error displayed to the user is that detail
- **AND** the UI does not replace it with only the HTTP status text
