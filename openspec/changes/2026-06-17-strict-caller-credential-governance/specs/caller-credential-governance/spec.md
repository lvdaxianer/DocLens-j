## MODIFIED Requirements

### Requirement: Caller identity MUST be resolved for authenticated requests

DocLens-j MUST resolve every request in this capability through a configured
caller credential before it serves data.

Every DocLens-j request that touches upload, query, mutation, or Dashboard
configuration surfaces MUST present a configured caller credential and resolve
to a trusted caller identity.

#### Scenario: API key credential is configured

- **WHEN** a caller sends `X-DocLens-Api-Key` matching a configured credential
- **THEN** the request is accepted
- **AND** the request resolves to the configured `client_id`
- **AND** the request resolves to the configured `source_app`
- **AND** the request resolves to the configured `tenant_key`

#### Scenario: Bearer token credential is configured

- **WHEN** a caller sends `Authorization: Bearer <token>` matching a configured
  credential
- **THEN** the request is accepted
- **AND** the request resolves to the configured `client_id`
- **AND** the request resolves to the configured `source_app`
- **AND** the request resolves to the configured `tenant_key`

#### Scenario: Request is missing a configured credential

- **WHEN** a caller sends a request without a matching configured credential
- **THEN** the request is rejected with HTTP 401
- **AND** no anonymous caller identity is created

### Requirement: Caller-scoped batch and document reads

DocLens-j MUST only expose batches, documents, events, and callback jobs that
belong to the resolved caller identity.

Dashboard and OCR read models MUST only expose data owned by the resolved caller
identity.

#### Scenario: Caller opens their own Dashboard summary

- **WHEN** a caller opens Dashboard summary or batch list
- **THEN** the response only includes batches created by that caller
- **AND** the response only includes documents, events, and callback jobs that
  belong to those batches

#### Scenario: Caller opens a foreign batch

- **WHEN** a caller requests a batch, document, result, event timeline, retry, or
  delete action for a resource that belongs to another caller
- **THEN** the API responds as not found
- **AND** no foreign data is returned

#### Scenario: Caller opens OCR health

- **WHEN** a caller opens OCR health
- **THEN** the response is calculated from that caller's own batches and
  documents only

### Requirement: Dashboard client forwards caller credential

The Dashboard frontend MUST attach the active caller credential to every API
request it sends.

The Dashboard frontend MUST read the active caller credential from
`localStorage` key `X-DocLens-Credential` and send it with every API request.

#### Scenario: Dashboard runtime provides API key

- **WHEN** `localStorage` contains `X-DocLens-Credential` with an API key
- **THEN** each API request sends `X-DocLens-Api-Key`

#### Scenario: Dashboard runtime provides Bearer token

- **WHEN** `localStorage` contains `X-DocLens-Credential` with a Bearer token
- **THEN** each API request sends `Authorization: Bearer <token>`

#### Scenario: Dashboard runtime is missing caller credential

- **WHEN** `localStorage` does not contain `X-DocLens-Credential`
- **THEN** the Dashboard sends no caller credential header
