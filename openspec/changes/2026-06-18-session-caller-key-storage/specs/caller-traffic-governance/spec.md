## MODIFIED Requirements

### Requirement: Dashboard MUST use X-DocLens-Credential-Key as the local credential key

The Dashboard frontend MUST read the active caller credential from
`sessionStorage` key `X-DocLens-Credential-Key`.

The Dashboard frontend MUST NOT read `localStorage` key
`X-DocLens-Credential-Key`.

The Dashboard frontend MUST NOT read `X-DocLens-Credential` after this change.

#### Scenario: Dashboard runtime provides API key credential

- **WHEN** `sessionStorage` contains `X-DocLens-Credential-Key` with value
  `local-api-key`
- **AND** the value does not start with `Bearer `
- **THEN** every Dashboard API request sends `X-DocLens-Api-Key:
  local-api-key`
- **AND** the request does not send an `Authorization` header for that
  credential

#### Scenario: Dashboard runtime provides Bearer credential

- **WHEN** `sessionStorage` contains `X-DocLens-Credential-Key` with value
  `Bearer local-token`
- **THEN** every Dashboard API request sends `Authorization:
  Bearer local-token`
- **AND** the request does not send `X-DocLens-Api-Key` for that credential

#### Scenario: Dashboard runtime only provides the same key in localStorage

- **WHEN** `localStorage` contains `X-DocLens-Credential-Key`
- **AND** `sessionStorage` does not contain `X-DocLens-Credential-Key`
- **THEN** Dashboard API requests send no caller credential header

#### Scenario: Dashboard runtime only provides the old key

- **WHEN** `localStorage` or `sessionStorage` contains
  `X-DocLens-Credential`
- **AND** `sessionStorage` does not contain `X-DocLens-Credential-Key`
- **THEN** Dashboard API requests send no caller credential header

### Requirement: Error responses MUST distinguish credential, ownership, caller limit, and global protection failures

DocLens-j MUST return distinct HTTP status codes for credential failures,
cross-caller resource access, caller/group limit failures, and global
protection failures.

Backend credential failures MUST be based on the configured credential
allowlist, not on the presence of any arbitrary caller key value.

#### Scenario: Request is missing or has invalid credential

- **WHEN** a request that requires DocLens caller identity has no matching
  configured credential
- **THEN** the API responds with HTTP `401`
- **AND** no anonymous caller identity is created

#### Scenario: Caller accesses foreign resource

- **WHEN** a valid caller requests a batch, document, result, event, retry, or
  delete action owned by another caller
- **THEN** the API responds with HTTP `404`
- **AND** no foreign resource data is returned

#### Scenario: Caller exceeds an interface group limit

- **WHEN** a valid caller exceeds the configured traffic limit for the request
  interface group
- **THEN** the API responds with HTTP `429`
- **AND** the response includes `Retry-After`
- **AND** the response includes `X-DocLens-Traffic-Group`
- **AND** the response does not include raw credential values

#### Scenario: Global protection rejects request

- **WHEN** global in-flight protection is enabled
- **AND** the configured global maximum is exceeded
- **THEN** the API responds with HTTP `503`
- **AND** the response does not consume caller interface-group tokens
