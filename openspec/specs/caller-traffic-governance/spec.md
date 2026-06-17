# caller-traffic-governance Specification

## Purpose
Define how DocLens-j uses caller credentials to identify calling systems and
protect service capacity with caller-scoped, interface-group-specific traffic
limits without introducing personal login or RBAC.
## Requirements
### Requirement: Dashboard MUST use X-DocLens-Credential-Key as the local credential key

The Dashboard frontend MUST read the active caller credential from
`localStorage` key `X-DocLens-Credential-Key`.

The Dashboard frontend MUST NOT read `X-DocLens-Credential` after this change.

#### Scenario: Dashboard runtime provides API key credential

- **WHEN** `localStorage` contains `X-DocLens-Credential-Key` with value
  `local-api-key`
- **AND** the value does not start with `Bearer `
- **THEN** every Dashboard API request sends `X-DocLens-Api-Key:
  local-api-key`
- **AND** the request does not send an `Authorization` header for that
  credential

#### Scenario: Dashboard runtime provides Bearer credential

- **WHEN** `localStorage` contains `X-DocLens-Credential-Key` with value
  `Bearer local-token`
- **THEN** every Dashboard API request sends `Authorization:
  Bearer local-token`
- **AND** the request does not send `X-DocLens-Api-Key` for that credential

#### Scenario: Dashboard runtime only provides the old key

- **WHEN** `localStorage` contains `X-DocLens-Credential`
- **AND** `localStorage` does not contain `X-DocLens-Credential-Key`
- **THEN** Dashboard API requests send no caller credential header

### Requirement: Caller traffic limits MUST be configurable in application.yml

DocLens-j MUST expose caller traffic governance configuration through Spring
configuration properties under `doclens.traffic` and
`doclens.clients.credentials[*].rate-limits`.

#### Scenario: Default interface group limit is configured

- **WHEN** `doclens.traffic.default-limits.dashboard-read.qps` is configured
- **AND** `doclens.traffic.default-limits.dashboard-read.burst` is configured
- **THEN** callers without an override use those values for `dashboard-read`

#### Scenario: Caller-specific interface group limit is configured

- **WHEN** a credential config contains `rate-limits.upload-write.qps`
- **AND** the same credential config contains `rate-limits.upload-write.burst`
- **THEN** that caller uses the credential-level values for `upload-write`
- **AND** other callers continue to use default limits

#### Scenario: Traffic governance is disabled

- **WHEN** `doclens.traffic.enabled` is `false`
- **THEN** valid caller requests are not rejected by caller traffic limits
- **AND** credential resolution and caller-scoped resource filtering still
  apply

### Requirement: Traffic limits MUST be isolated by caller and interface group

DocLens-j MUST enforce limits by resolved caller identity and interface group.

Interface groups MUST include `dashboard-read`, `detail-read`, `upload-write`,
`ocr-mutation`, `config-mutation`, and `admin-health`.

#### Scenario: One caller exceeds upload limit

- **WHEN** caller A exceeds the configured `upload-write` limit
- **THEN** caller A receives HTTP `429` for additional `upload-write` requests
- **AND** caller A can still use remaining `dashboard-read` capacity
- **AND** caller B is not affected

#### Scenario: Interface groups have different budgets

- **WHEN** `dashboard-read` is configured with higher QPS than `upload-write`
- **THEN** Dashboard read requests are allowed at the higher read budget
- **AND** upload requests are rejected once the lower upload budget is exceeded

#### Scenario: Request path has no explicit group mapping

- **WHEN** an authenticated request does not match a known traffic group
- **THEN** DocLens-j classifies it as `detail-read`
- **AND** applies the `detail-read` limit

### Requirement: Error responses MUST distinguish credential, ownership, caller limit, and global protection failures

DocLens-j MUST return distinct HTTP status codes for credential failures,
cross-caller resource access, caller/group limit failures, and global
protection failures.

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
