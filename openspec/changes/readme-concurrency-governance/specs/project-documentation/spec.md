## MODIFIED Requirements

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
