# reliability Specification

## Purpose
Defines trusted-gateway, principal, partition-key, admin-route, and recovery-error reliability requirements for DocLens-j request isolation.

## Requirements
### Requirement: Partition key is data isolation only

DocLens-j MUST treat `X-Doclens-Key` as a partition key and MUST NOT treat it
as an authentication secret.

#### Scenario: Request provides a partition key

- **WHEN** a request sends a non-empty `X-Doclens-Key`
- **THEN** DocLens-j uses it as the caller partition
- **AND** the key is not treated as proof of identity

#### Scenario: Request omits the partition key

- **WHEN** a request that touches upload, query, mutation, or Dashboard data
  does not send `X-Doclens-Key`
- **THEN** the request is rejected with a stable error

### Requirement: Trusted gateway must be proven

DocLens-j MUST reject requests unless they prove they came through the trusted
gateway and carry a trusted principal.

#### Scenario: Gateway proof is missing

- **WHEN** a request lacks the configured gateway proof
- **THEN** the request is rejected with HTTP 401

#### Scenario: Principal is missing

- **WHEN** a request has gateway proof but no principal header
- **THEN** the request is rejected with HTTP 401

### Requirement: Principal must be allowed for the partition

DocLens-j MUST only accept a partition key when the authenticated principal is
allowed to access it.

#### Scenario: Principal accesses an allowed partition

- **WHEN** the principal is configured for the requested partition
- **THEN** the request continues

#### Scenario: Principal accesses a denied partition

- **WHEN** the principal is not configured for the requested partition
- **THEN** the request is rejected with HTTP 403

### Requirement: Admin routes require admin role

DocLens-j MUST require admin role for governance and callback retry routes.

#### Scenario: User calls an admin route

- **WHEN** a non-admin principal calls a configured admin route
- **THEN** the request is rejected with HTTP 403

### Requirement: Dashboard surfaces stable recovery errors

The Dashboard MUST be able to distinguish missing partition, permission denied,
rate limited, validation failure, and server failure.

#### Scenario: Backend returns missing partition

- **WHEN** the backend responds with a missing-partition error
- **THEN** the frontend can render a partition recovery prompt

#### Scenario: Backend returns permission denied

- **WHEN** the backend responds with a permission denied error
- **THEN** the frontend can render a permission recovery prompt
