## Context

The current project has three database paths:

- Development profile uses H2 file storage at `./var/db/doclens`.
- Test resources and many test classes use H2 memory or file databases with
  `MODE=PostgreSQL`.
- Production profile delegates datasource values to `DOCLENS_DB_*` environment
  variables and the server module already carries the PostgreSQL JDBC driver.

The H2 paths are convenient but they do not provide the same behavior as
PostgreSQL. The requested new baseline is PostgreSQL everywhere.

## Goals / Non-Goals

**Goals:**

- Make local development start against PostgreSQL, not H2.
- Make automated tests run against PostgreSQL, not H2 compatibility mode.
- Remove H2 datasource URLs, H2 driver class names, and H2 dependencies from the
  supported runtime and test paths.
- Keep production datasource configuration environment-driven while documenting
  PostgreSQL as the expected driver and URL form.
- Provide a repeatable local PostgreSQL service for developers.

**Non-Goals:**

- Do not redesign the persistence layer or replace MyBatis/JDBC.
- Do not change public API behavior.
- Do not change business schemas except when a Flyway or SQL statement must be
  corrected to run on PostgreSQL.
- Do not migrate existing local H2 data files automatically.

## Decisions

- Use Docker Compose for local PostgreSQL so developers have a consistent
  database without installing PostgreSQL manually.
- Use Testcontainers PostgreSQL for automated tests so local and CI test runs do
  not depend on a pre-existing database.
- Add shared PostgreSQL test support per Java module where needed, then migrate
  H2 overrides to that support instead of duplicating container setup in every
  test.
- Keep production datasource values under `DOCLENS_DB_URL`,
  `DOCLENS_DB_USERNAME`, `DOCLENS_DB_PASSWORD`, and `DOCLENS_DB_DRIVER`, with
  `org.postgresql.Driver` as the documented driver value.
- Add guardrail tests or verification checks that fail if `jdbc:h2`,
  `org.h2.Driver`, or `com.h2database:h2` returns to active configuration.

## Risks / Trade-offs

- Testcontainers requires Docker or a compatible container runtime during test
  execution.
- PostgreSQL-backed tests can be slower than H2 tests; shared reusable test
  containers should limit startup cost.
- Real PostgreSQL may expose migration or SQL issues currently hidden by H2.
  Those issues should be fixed in the same task that migrates the affected test
  area.
- Existing developer H2 data under `./var/db/doclens` will not be reused by the
  new PostgreSQL development database.
