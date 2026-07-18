# project-documentation Specification

## Purpose
Define the expected shape of DocLens Java project documentation so the root
READMEs remain concise entry points and detailed API, SDK, configuration,
development, packaging, and integration references stay discoverable.
## Requirements
### Requirement: Bilingual README Overview

The project SHALL provide Chinese and English README entry points that describe
DocLens Java at a project overview level and link to focused reference
documents for details. The canonical English README path SHALL be
`README-en.md`.

#### Scenario: Reader starts from the Chinese README

- **WHEN** a reader opens `README.md`
- **THEN** they can understand the project purpose, deployment modes, modules,
  quick start path, dashboard entry, supported document pipeline, OCR
  concurrency model, global load balancing preference, restart recovery model,
  LLM Markdown checkpoint behavior, upload guardrails, observability surface,
  and where to find API, SDK, configuration, development, packaging,
  integration, and technical delivery details

#### Scenario: Reader starts from the English README

- **WHEN** a reader opens `README-en.md`
- **THEN** they can access equivalent overview, current capability summary, and
  reference links in English

### Requirement: Focused HTTP API Reference

The project SHALL provide a focused HTTP API reference document that lists the
main REST endpoint groups exposed by `doclens-server`.

#### Scenario: Reader needs native DocLens endpoints

- **WHEN** a reader opens the API reference
- **THEN** they can find batch upload/query, document query/result/retry/delete,
  adapter, health, dashboard, OCR model/node, governance, and LLM Markdown
  configuration endpoint groups

#### Scenario: Reader needs integration endpoints

- **WHEN** a reader opens the API reference
- **THEN** they can find the OpenWebUI OCR integration endpoint prefix and a
  link to the detailed OpenWebUI contract

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
- **AND** the default server configuration examples focus on shared OCR
  routing, worker, extraction, rendering, callback, and traffic controls
- **AND** the configuration document does not present
  `doclens.adapter.default-key`, `doclens.paddle-ocr.enabled`,
  `doclens.paddle-ocr.timeout-seconds`, or
  `doclens.paddle-ocr.visualize` as editable default runtime keys

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
- **AND** the packaging document explains how to override common container
  runtime settings through environment variables
- **AND** the packaging document explains how to mount an external Spring
  configuration directory for larger runtime overrides
- **AND** the packaging document includes a dedicated all-in-one Docker Compose
  example workflow with env and mounted config example files

#### Scenario: Reader wants delivery architecture and high availability
- **WHEN** a reader needs to evaluate DocLens-j as a delivered technical system
- **THEN** they can find a technical delivery document that explains system
  positioning, architecture, high-volume concurrency, OCR load balancing,
  restart recovery, chunk checkpoint persistence, duplicate-consumption
  protection, upload guardrails, observability, capacity tuning, and
  deliberate technical trade-offs

#### Scenario: Reader wants a full high-volume processing flow
- **WHEN** a reader needs to understand what happens after many documents are
  uploaded at the same time
- **THEN** the technical delivery document includes a flowchart or equivalent
  structured explanation that follows documents from upload acknowledgement
  through document executor queueing, page-task persistence, OCR slot
  scheduling, retry or failure handling, aggregation, optional LLM processing,
  and final batch completion

