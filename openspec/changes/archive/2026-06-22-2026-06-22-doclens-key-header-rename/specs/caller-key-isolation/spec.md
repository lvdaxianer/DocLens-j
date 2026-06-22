## MODIFIED Requirements

### Requirement: Caller partition key MUST be resolved from X-Doclens-Key

DocLens-j MUST treat `X-Doclens-Key` as an opaque partition key
provided by the caller.

DocLens-j MUST NOT validate the key against a backend allowlist, API key list,
Bearer token list, user table, workspace table, or RBAC rule set.

DocLens-j MUST NOT resolve caller partitions from `X-Recall-Key`.

#### Scenario: Request provides a non-blank partition key

- **WHEN** a request sends `X-Doclens-Key: tenant-a`
- **THEN** DocLens-j accepts the key as the current caller partition
- **AND** no backend credential lookup is required
- **AND** the resolved caller identity uses `tenant-a` as the partition value

#### Scenario: Request omits the partition key

- **WHEN** a request that touches upload, query, mutation, or Dashboard data does
  not send `X-Doclens-Key`
- **THEN** the request is rejected
- **AND** no anonymous shared partition is created

#### Scenario: Request provides a blank partition key

- **WHEN** a request sends `X-Doclens-Key` with only blank text
- **THEN** the request is rejected
- **AND** no anonymous shared partition is created

#### Scenario: Request provides only the old wrong header

- **WHEN** a request sends `X-Recall-Key: tenant-a`
- **AND** does not send `X-Doclens-Key`
- **THEN** the request is rejected
- **AND** no caller partition is resolved from `X-Recall-Key`

### Requirement: Dashboard MUST forward the raw partition key

The Dashboard frontend MUST read `X-Doclens-Key` from
`sessionStorage` and forward it as the same request header.

The Dashboard frontend MUST NOT translate this value into `X-DocLens-Api-Key`
or `Authorization`.

The Dashboard frontend MUST NOT send `X-Recall-Key` or
`X-DocLens-Credential-Key` for caller partitioning after this change.

#### Scenario: Dashboard has a partition key in session storage

- **WHEN** `sessionStorage` contains `X-Doclens-Key` with value
  `tenant-a`
- **THEN** each Dashboard API request sends
  `X-Doclens-Key: tenant-a`
- **AND** the request does not add `X-DocLens-Api-Key`
- **AND** the request does not add `Authorization`
- **AND** the request does not add `X-Recall-Key`
- **AND** the request does not add `X-DocLens-Credential-Key`

#### Scenario: Dashboard has only legacy local storage keys

- **WHEN** only `localStorage` contains `X-Doclens-Key`, `X-Recall-Key`,
  `X-DocLens-Credential-Key`, or `X-DocLens-Credential`
- **THEN** Dashboard API requests send no caller partition header

#### Scenario: Dashboard has only the old wrong key in session storage

- **WHEN** only `sessionStorage` contains `X-Recall-Key`
- **THEN** Dashboard API requests send no caller partition header
