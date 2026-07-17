## MODIFIED Requirements

### Requirement: Focused Operational Reference Documents
The project SHALL provide focused reference documents for SDK usage,
configuration, bilingual permission configuration, local development workflows,
packaging and deployment workflows, and technical delivery.

#### Scenario: Reader wants embedded usage
- **WHEN** a reader needs to embed DocLens in a host application
- **THEN** they can find Spring Boot starter usage and pure Java SDK notes in
  the SDK document

#### Scenario: Reader wants runtime settings
- **WHEN** a reader needs to configure DocLens
- **THEN** they can find key `doclens.*` settings, PostgreSQL datasource
  environment variables, and environment-variable secret conventions in the
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
- **THEN** they can find dev scripts, service ports, logs, verification
  commands, and PostgreSQL startup/configuration steps in the development
  document

#### Scenario: Reader wants packaging and deployment commands
- **WHEN** a reader needs to package or deploy DocLens-j
- **THEN** they can find Maven Assembly distribution, all-in-one Docker image,
  and Helm chart commands in the packaging document

#### Scenario: Reader wants delivery architecture and high availability
- **WHEN** a reader needs to evaluate DocLens-j as a delivered technical system
- **THEN** they can find a technical delivery document that explains system
  positioning, architecture, high-volume concurrency, OCR load balancing,
  restart recovery, chunk checkpoint persistence, duplicate-consumption
  protection, upload guardrails, observability, capacity tuning, and deliberate
  technical trade-offs

#### Scenario: Reader wants a full high-volume processing flow
- **WHEN** a reader needs to understand what happens after many documents are
  uploaded at the same time
- **THEN** the technical delivery document includes a flowchart or equivalent
  structured explanation that follows documents from upload acknowledgement
  through document executor queueing, page-task persistence, OCR slot
  scheduling, retry or failure handling, aggregation, optional LLM processing,
  and final batch completion
