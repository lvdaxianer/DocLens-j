## MODIFIED Requirements

### Requirement: Focused Operational Reference Documents

The project SHALL provide focused reference documents for SDK usage,
configuration, permission configuration, local development workflows, and
technical delivery.

#### Scenario: Reader wants embedded usage

- **WHEN** a reader needs to embed DocLens in a host application
- **THEN** they can find Spring Boot starter usage and pure Java SDK notes in
  the SDK document

#### Scenario: Reader wants runtime settings

- **WHEN** a reader needs to configure DocLens
- **THEN** they can find key `doclens.*` settings and environment-variable
  secret conventions in the configuration document

#### Scenario: Reader wants permission setup

- **WHEN** a reader needs to configure API access for production or development
- **THEN** they can find trusted gateway authentication, principal allowlist,
  caller partition key, admin route, local development, curl, and failure
  response examples in the permission configuration document

#### Scenario: Reader wants local development commands

- **WHEN** a reader needs to run the project locally
- **THEN** they can find dev scripts, service ports, logs, and verification
  commands in the development document

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
