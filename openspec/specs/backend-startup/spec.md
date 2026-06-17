# backend-startup Specification

## Purpose
Make the DocLens backend reliably start through the existing local dev
restart flow by keeping Spring configuration binding for
`DocLensSpringProperties` constructor-based and unambiguous.

## Requirements

### Requirement: DocLensSpringProperties binds without a default constructor
The Spring Boot application MUST be able to instantiate
`DocLensSpringProperties` from configuration values without requiring a
default constructor.

#### Scenario: Minimal configuration is enough
- **WHEN** Spring Boot starts the starter context with only the required
  `doclens.*` values for `storageRoot`, `autoProcessOnUpload`, and `workerId`
- **THEN** `DocLensSpringProperties` is created successfully
- **AND** the bean is bound through its canonical constructor
- **AND** the context does not fail with a default-constructor binding error

### Requirement: Dev restart launches the backend
The existing `./scripts/dev-restart.sh` flow MUST start the backend process
successfully after the project is rebuilt.

#### Scenario: Dev restart reaches a live backend
- **WHEN** a developer runs `./scripts/dev-restart.sh`
- **THEN** the backend process starts without reporting a startup failure
- **AND** the health endpoint on port `10003` becomes reachable
- **AND** the startup log does not end with the `DocLensSpringProperties`
  binding failure seen before this fix
