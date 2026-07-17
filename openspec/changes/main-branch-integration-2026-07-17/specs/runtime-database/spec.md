## ADDED Requirements

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
