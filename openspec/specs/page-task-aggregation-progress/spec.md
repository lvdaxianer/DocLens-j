# page-task-aggregation-progress Specification

## Purpose
Ensure page-task OCR aggregation exposes accurate post-OCR progress while slow
Markdown LLM formatting is running.
## Requirements
### Requirement: Page-task aggregation exposes post-OCR progress before LLM work
When all page OCR tasks for a document have completed, the system SHALL persist a
document stage transition out of `OCR_IMAGES` before invoking Markdown LLM
post-processing.

#### Scenario: Final page completion reaches LLM stage before Markdown returns
- **GIVEN** a document is processed through page-task OCR
- **AND** all page OCR results are available
- **AND** Markdown post-processing is slow or blocked
- **WHEN** the final page success callback triggers document aggregation
- **THEN** the persisted document stage is advanced to `SAVE_TEXT` before the
  Markdown post-processor returns
- **AND** the persisted page progress shows all pages completed
- **AND** Markdown post-processing still runs outside the database transaction
