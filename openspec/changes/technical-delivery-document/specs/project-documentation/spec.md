## MODIFIED Requirements

### Requirement: Focused Operational Reference Documents

The project SHALL provide focused reference documents for SDK usage,
configuration, local development workflows, and technical delivery.

#### Scenario: Reader wants embedded usage

- **WHEN** a reader needs to embed DocLens in a host application
- **THEN** they can find Spring Boot starter usage and pure Java SDK notes in
  the SDK document

#### Scenario: Reader wants runtime settings

- **WHEN** a reader needs to configure DocLens
- **THEN** they can find key `doclens.*` settings and environment-variable
  secret conventions in the configuration document

#### Scenario: Reader wants local development commands

- **WHEN** a reader needs to run the project locally
- **THEN** they can find dev scripts, service ports, logs, and verification
  commands in the development document

#### Scenario: Reader wants delivery architecture and high availability

- **WHEN** a reader needs to evaluate DocLens-j as a delivered technical system
- **THEN** they can find a technical delivery document that explains system
  positioning, architecture, high-volume concurrency, OCR load balancing,
  high-availability controls, duplicate-consumption protection, observability,
  capacity tuning, and deliberate technical trade-offs
