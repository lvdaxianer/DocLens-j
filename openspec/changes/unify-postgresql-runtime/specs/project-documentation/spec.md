## MODIFIED Requirements

### Requirement: Focused Operational Reference Documents

The project SHALL provide focused reference documents for SDK usage,
configuration, bilingual permission configuration, local development workflows,
and technical delivery.

#### Scenario: Reader wants local development commands

- **WHEN** a reader needs to run the project locally
- **THEN** they can find dev scripts, service ports, logs, verification
  commands, and PostgreSQL startup/configuration steps in the development
  document

#### Scenario: Reader wants runtime settings

- **WHEN** a reader needs to configure DocLens
- **THEN** they can find key `doclens.*` settings, PostgreSQL datasource
  environment variables, and environment-variable secret conventions in the
  configuration document
