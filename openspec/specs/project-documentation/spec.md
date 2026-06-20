# project-documentation Specification

## Purpose
Define the expected shape of DocLens Java project documentation so the root
READMEs remain concise entry points and detailed API, SDK, configuration,
development, packaging, and integration references stay discoverable.
## Requirements
### Requirement: Bilingual README Overview

The project SHALL provide Chinese and English README entry points that describe
DocLens Java at a project overview level and link to focused reference
documents for details. The Chinese README SHALL also summarize the runtime
concurrency and OCR governance model so operators can understand high-volume
batch behavior from the root document.

#### Scenario: Reader starts from the Chinese README

- **WHEN** a reader opens `README.md`
- **THEN** they can understand the project purpose, modules, quick start path,
  document processing pipeline, concurrency controls, OCR load balancing,
  retries, circuit breaking, duplicate-consumption protection, and where to find
  API, SDK, configuration, development, packaging, and integration details

#### Scenario: Reader starts from the English README

- **WHEN** a reader opens the English README
- **THEN** they can access equivalent overview and reference links in English

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
configuration, and local development workflows.

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

