## ADDED Requirements

### Requirement: Caller partition key MUST be resolved from X-Recall-Key

DocLens-j MUST treat `X-Recall-Key` as an opaque partition key
provided by the caller.

DocLens-j MUST NOT validate the key against a backend allowlist, API key list,
Bearer token list, user table, workspace table, or RBAC rule set.

#### Scenario: Request provides a non-blank partition key

- **WHEN** a request sends `X-Recall-Key: tenant-a`
- **THEN** DocLens-j accepts the key as the current caller partition
- **AND** no backend credential lookup is required
- **AND** the resolved caller identity uses `tenant-a` as the partition value

#### Scenario: Request omits the partition key

- **WHEN** a request that touches upload, query, mutation, or Dashboard data does
  not send `X-Recall-Key`
- **THEN** the request is rejected
- **AND** no anonymous shared partition is created

#### Scenario: Request provides a blank partition key

- **WHEN** a request sends `X-Recall-Key` with only blank text
- **THEN** the request is rejected
- **AND** no anonymous shared partition is created

### Requirement: Dashboard MUST forward the raw partition key

The Dashboard frontend MUST read `X-Recall-Key` from
`sessionStorage` and forward it as the same request header.

The Dashboard frontend MUST NOT translate this value into `X-DocLens-Api-Key`
or `Authorization`.

The Dashboard frontend MUST NOT send `X-DocLens-Credential-Key` for caller
partitioning after this change.

#### Scenario: Dashboard has a partition key in session storage

- **WHEN** `sessionStorage` contains `X-Recall-Key` with value
  `tenant-a`
- **THEN** each Dashboard API request sends
  `X-Recall-Key: tenant-a`
- **AND** the request does not add `X-DocLens-Api-Key`
- **AND** the request does not add `Authorization`
- **AND** the request does not add `X-DocLens-Credential-Key`

#### Scenario: Dashboard has only legacy local storage keys

- **WHEN** only `localStorage` contains `X-Recall-Key`,
  `X-DocLens-Credential-Key`, or `X-DocLens-Credential`
- **THEN** Dashboard API requests send no caller partition header

### Requirement: Resources MUST be isolated by caller partition key

DocLens-j MUST only expose batches, documents, results, events, retries, and
deletes that belong to the current caller partition key.

#### Scenario: Caller opens a resource in the same partition

- **WHEN** caller partition `tenant-a` requests a batch created under
  `tenant-a`
- **THEN** DocLens-j returns the batch data

#### Scenario: Caller opens a resource in another partition

- **WHEN** caller partition `tenant-b` requests a batch created under
  `tenant-a`
- **THEN** DocLens-j responds as if the resource was not found
- **AND** no foreign resource data is returned

### Requirement: Traffic limits MUST use caller partition buckets

DocLens-j MUST enforce caller traffic limits by caller partition key and
interface group.

Global in-flight protection MUST remain available as a stop-loss layer when a
caller changes partition keys to avoid per-key buckets.

#### Scenario: One partition exceeds upload limit

- **WHEN** partition `tenant-a` exceeds the configured `upload-write` limit
- **THEN** additional `upload-write` requests from `tenant-a` receive HTTP `429`
- **AND** partition `tenant-b` can still use its own `upload-write` bucket

#### Scenario: Global protection rejects traffic

- **WHEN** global in-flight protection is enabled
- **AND** the configured global maximum is exceeded
- **THEN** the API responds with HTTP `503`
- **AND** the response does not depend on whether the caller changed partition
  keys
