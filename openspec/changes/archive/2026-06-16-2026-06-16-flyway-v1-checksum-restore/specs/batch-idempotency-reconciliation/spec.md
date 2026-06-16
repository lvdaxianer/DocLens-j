## MODIFIED Requirements

### Requirement: Idempotency key pass-through
DocLens-j MUST treat uploaded `idempotency_key` values as third-party
pass-through values and MUST NOT use them to reject duplicate uploads. Versioned
Flyway migrations that have already been released MUST remain checksum-stable;
schema changes that remove uniqueness MUST be applied by later migrations.

#### Scenario: Published migration checksum remains stable
- **WHEN** a local database has already applied V1
- **AND** newer migrations are available
- **THEN** application startup can validate V1 without checksum mismatch
- **AND** a later migration removes DocLens-side uniqueness for `idempotency_key`
