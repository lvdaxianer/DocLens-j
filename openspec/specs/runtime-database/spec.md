# runtime-database Specification

## Purpose
Define the supported database runtime baseline for DocLens-j so local
development, automated tests, and production use PostgreSQL consistently and no
active H2 runtime or test path remains.
## Requirements
### Requirement: Runtime database MUST be PostgreSQL

DocLens-j MUST use PostgreSQL as the database for local development,
automated tests, and production runtime.

#### Scenario: Local development starts with PostgreSQL

- **WHEN** a developer starts DocLens-j with the development profile
- **THEN** the configured datasource uses a `jdbc:postgresql:` URL
- **AND** the configured datasource driver is `org.postgresql.Driver`
- **AND** the development profile does not configure H2

#### Scenario: Automated tests start with PostgreSQL

- **WHEN** automated Java tests need a datasource
- **THEN** the tests obtain a PostgreSQL datasource
- **AND** the tests do not configure `jdbc:h2:` URLs
- **AND** the tests do not use `org.h2.Driver`

#### Scenario: Production starts with PostgreSQL

- **WHEN** the production profile is configured through environment variables
- **THEN** the expected datasource URL uses the PostgreSQL JDBC form
- **AND** the expected datasource driver is `org.postgresql.Driver`

### Requirement: H2 MUST NOT be an active dependency

DocLens-j MUST NOT depend on H2 for supported runtime or automated test
execution after PostgreSQL-only runtime is adopted.

#### Scenario: Build dependencies are inspected

- **WHEN** maintainers inspect Maven runtime and test dependencies
- **THEN** H2 is not declared as a runtime or test dependency
- **AND** PostgreSQL JDBC remains available where database access is required

#### Scenario: Repository configuration is scanned

- **WHEN** maintainers scan active source, configuration, and documentation
  paths
- **THEN** no supported runtime or automated test path instructs DocLens-j to use
  H2
- **AND** references to historical H2 behavior, if any remain, are clearly
  marked as obsolete or migration notes
### Requirement: PostgreSQL is the required runtime database
DocLens SHALL use PostgreSQL as the required persistence backend for both local
runtime and production deployment on `main`.

#### Scenario: Local runtime uses PostgreSQL
- **WHEN** an operator starts DocLens from the supported local runtime assets on
  `main`
- **THEN** the runtime configuration points at PostgreSQL rather than an
  alternative embedded or standalone database

#### Scenario: Production deployment uses PostgreSQL
- **WHEN** an operator deploys DocLens from the production assets on `main`
- **THEN** the deployment requires PostgreSQL-backed configuration and
  initialization inputs
