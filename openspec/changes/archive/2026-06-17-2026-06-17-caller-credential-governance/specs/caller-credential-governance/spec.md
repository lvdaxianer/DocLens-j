# caller-credential-governance Specification

## Purpose

Define the Stage 2 caller credential and governance behavior that lets
DocLens-j attribute uploads and callbacks to calling systems without taking
over personal login, user accounts, or RBAC.

## ADDED Requirements

### Requirement: Caller identity is resolved for new batches

Every new native upload batch MUST have a trusted caller identity containing
`client_id`, `source_app`, and `tenant_key` fields.

#### Scenario: No credentials are configured

- **WHEN** a caller creates a batch and no DocLens caller credentials are
  configured
- **THEN** the request is accepted
- **AND** the created batch is attributed to `client_id` `anonymous`
- **AND** the created batch is attributed to `source_app` `unknown`
- **AND** the created batch has an empty `tenant_key`

#### Scenario: API key credential is configured

- **WHEN** a caller creates a batch with `X-DocLens-Api-Key` matching a
  configured credential
- **THEN** the request is accepted
- **AND** the created batch is attributed to the configured `client_id`
- **AND** the created batch is attributed to the configured `source_app`
- **AND** the created batch is attributed to the configured `tenant_key`

#### Scenario: Bearer token credential is configured

- **WHEN** a caller creates a batch with `Authorization: Bearer <token>`
  matching a configured credential
- **THEN** the request is accepted
- **AND** the created batch is attributed to the configured `client_id`
- **AND** the created batch is attributed to the configured `source_app`
- **AND** the created batch is attributed to the configured `tenant_key`

#### Scenario: Credentials are configured but request is missing credentials

- **WHEN** a caller creates a batch without a matching configured credential
- **THEN** the request is rejected with HTTP 401
- **AND** no batch is created

### Requirement: Caller identity is queryable

Batch query surfaces MUST expose caller identity for operational diagnosis.

#### Scenario: Dashboard batch detail is queried

- **WHEN** an operator opens a Dashboard batch detail page
- **THEN** the response `batch` object contains `client_id`
- **AND** the response `batch` object contains `source_app`
- **AND** the response `batch` object contains `tenant_key`

#### Scenario: Dashboard batch list is queried

- **WHEN** an operator opens the Dashboard batch list or summary
- **THEN** every returned batch row contains `client_id`
- **AND** every returned batch row contains `source_app`
- **AND** every returned batch row contains `tenant_key`

### Requirement: Callback payload includes caller identity

Completed-document callbacks MUST include the caller identity from the owning
batch.

#### Scenario: Completed document callback is created

- **WHEN** DocLens-j creates a completed-document callback payload for a batch
  with caller identity
- **THEN** the payload contains a `caller` object
- **AND** `caller.client_id` equals the batch caller `client_id`
- **AND** `caller.source_app` equals the batch caller `source_app`
- **AND** `caller.tenant_key` equals the batch caller `tenant_key`

### Requirement: Dashboard displays caller identity

The Dashboard batch intake panel MUST show caller identity without representing
it as a personal user login.

#### Scenario: Batch has caller identity

- **WHEN** an operator opens batch detail for a batch with caller identity
- **THEN** the intake panel displays `client_id`
- **AND** the intake panel displays `source_app`
- **AND** the intake panel displays `tenant_key`

#### Scenario: Batch has no tenant key

- **WHEN** an operator opens batch detail for a batch without `tenant_key`
- **THEN** the intake panel displays a stable empty placeholder for
  `tenant_key`
