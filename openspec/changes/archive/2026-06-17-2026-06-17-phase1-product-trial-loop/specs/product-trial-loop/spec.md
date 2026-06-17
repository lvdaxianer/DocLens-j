## ADDED Requirements

### Requirement: Result downloads

The Dashboard MUST let users download a loaded document result as Markdown,
TXT, or JSON without requiring a new backend endpoint.

#### Scenario: Completed result has download actions

- **WHEN** a user opens the document result drawer for a loaded result
- **THEN** the drawer exposes Markdown, TXT, and JSON download actions
- **AND** the Markdown download contains the final result text
- **AND** the TXT download contains the OCR original text when present, or the
  final result text otherwise
- **AND** the JSON download contains the loaded result payload
- **AND** download filenames include the source document name and result ID

#### Scenario: Result text is not loaded

- **WHEN** the drawer has no loaded result
- **THEN** the download actions are not enabled

### Requirement: Five-minute trial guide

DocLens-j MUST provide a concise guide that helps a new user start the local
services, upload a document, inspect the batch, and download a result.

#### Scenario: Reader follows the trial guide

- **WHEN** a reader opens `docs/quick-trial.md`
- **THEN** the guide lists local startup commands
- **AND** the guide points to the Dashboard upload URL
- **AND** the guide explains how to open batch detail and download Markdown,
  TXT, or JSON results

### Requirement: Trial guide entry point

The Dashboard upload page MUST surface the five-minute trial guide without
blocking the upload workflow.

#### Scenario: New user opens upload page

- **WHEN** a user opens the Dashboard upload page
- **THEN** the page includes a visible link to `docs/quick-trial.md`
- **AND** the upload form remains usable without interacting with the guide
