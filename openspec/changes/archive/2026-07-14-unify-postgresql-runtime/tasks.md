## 1. PostgreSQL test foundation

- [x] 1.1 Add and satisfy PostgreSQL profile guardrails plus shared
  Testcontainers datasource foundations for the affected Java modules.

## 2. Server contract tests

- [x] 2.1 Migrate `doclens-server` contract tests and support classes from H2
  dynamic datasource overrides to PostgreSQL Testcontainers.
- [x] 2.2 Run focused server contract/profile verification and fix PostgreSQL SQL
  or migration issues found by that migration.

## 3. Starter repository and schema tests

- [x] 3.1 Migrate `doclens-spring-boot-starter` repository, schema, wiring, and
  embedded starter tests from H2 to PostgreSQL Testcontainers.
- [x] 3.2 Run focused starter verification and fix PostgreSQL SQL or migration
  issues found by that migration.

## 4. Runtime configuration and documentation cleanup

- [x] 4.1 Update local development configuration and Docker Compose support so
  local runs use PostgreSQL by default.
- [x] 4.2 Remove H2 dependencies and all remaining active H2 datasource or driver
  references from the repository.
- [x] 4.3 Update README and docs references so PostgreSQL is documented as the
  only supported database.
- [x] 4.4 Run broad Maven verification plus repository scans proving no active
  H2 runtime or test configuration remains.
