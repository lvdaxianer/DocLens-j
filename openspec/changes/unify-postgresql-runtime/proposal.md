## Why

DocLens-j currently uses H2 for local development and automated tests while
production is configured through environment variables and is expected to use
PostgreSQL. H2's PostgreSQL compatibility mode still differs from real
PostgreSQL in SQL behavior, type handling, locking, indexing, Flyway execution,
and case sensitivity. That split can hide runtime defects until production.

The project is being revised so every supported runtime path uses PostgreSQL:
local development, automated tests, and production.

## What Changes

Replace all H2-backed development and test datasource paths with PostgreSQL.
Add local PostgreSQL startup support, move automated tests to PostgreSQL through
Testcontainers, and remove H2 runtime/test dependencies once the test suite no
longer needs them.

Update documentation and configuration examples so operators and developers see
PostgreSQL as the only supported database for DocLens-j.

## Capabilities

### New Capabilities

- `runtime-database`: Defines the database runtime requirement for local,
  automated test, and production environments.

### Modified Capabilities

- `project-documentation`: The local development and configuration references
  must describe PostgreSQL-only runtime setup instead of H2-backed local or
  test behavior.

## Impact

- `doclens-server` application configuration and runtime dependencies.
- `doclens-server` contract and profile tests that currently override H2
  datasource URLs.
- `doclens-spring-boot-starter` repository, schema, wiring, and embedded
  starter tests that currently use H2.
- Local development bootstrap files such as Docker Compose or environment
  examples.
- Documentation under `README*` and `docs/` that mentions local database setup,
  configuration, or test database behavior.
