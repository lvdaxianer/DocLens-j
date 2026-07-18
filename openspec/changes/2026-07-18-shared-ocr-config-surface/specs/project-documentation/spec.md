## MODIFIED Requirements

### Requirement: Focused Operational Reference Documents
The project SHALL provide focused reference documents for SDK usage,
configuration, bilingual permission configuration, local development workflows,
packaging and deployment workflows, and technical delivery.

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
