## MODIFIED Requirements

### Requirement: Focused Operational Reference Documents
The project SHALL provide focused reference documents for SDK usage,
configuration, bilingual permission configuration, local development workflows,
technical delivery, PostgreSQL runtime expectations, and packaging/deployment
references.

#### Scenario: Reader wants runtime settings
- **WHEN** a reader needs to configure DocLens
- **THEN** they can find key `doclens.*` settings, PostgreSQL runtime
  expectations, and environment-variable secret conventions in the
  configuration document

#### Scenario: Reader wants permission setup in English
- **WHEN** a reader needs to configure API access for production or development
  in English
- **THEN** they can find trusted gateway authentication, principal allowlist,
  caller partition key, role meaning, allowed-partition examples, admin route,
  local development, curl, and failure response examples in the English
  permission configuration document

#### Scenario: Reader wants permission setup in Chinese
- **WHEN** a reader needs to configure API access for production or development
  in Chinese
- **THEN** they can find trusted gateway authentication, principal allowlist,
  caller partition key, role meaning, allowed-partition examples, admin route,
  local development, curl, and failure response examples in the Chinese
  permission configuration document

#### Scenario: Reader wants local development commands
- **WHEN** a reader needs to run the project locally
- **THEN** they can find dev scripts, service ports, logs, PostgreSQL startup
  expectations, and verification commands in the development document

#### Scenario: Reader wants packaging and delivery references
- **WHEN** a reader needs to build or deploy DocLens from `main`
- **THEN** they can find container packaging, SQL initialization, and Helm
  delivery references for the PostgreSQL-based runtime
