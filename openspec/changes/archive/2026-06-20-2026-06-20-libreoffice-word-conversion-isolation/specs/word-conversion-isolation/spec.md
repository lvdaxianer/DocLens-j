## ADDED Requirements

### Requirement: Word conversion isolates LibreOffice runtime state

Each Word-to-PDF conversion MUST run LibreOffice with a conversion-specific user installation profile so concurrent Word conversions do not share the default LibreOffice profile.

#### Scenario: Concurrent Word conversions receive separate LibreOffice profiles

- **WHEN** multiple Word documents are prepared for OCR in the same batch
- **THEN** each LibreOffice conversion process is launched with `-env:UserInstallation=file://<conversion-specific-profile-dir>`
- **AND** the profile directory is scoped to that conversion's temporary workspace
- **AND** the conversion continues to produce the same PDF bytes when LibreOffice succeeds

### Requirement: Word conversion failures expose LibreOffice diagnostics

Word conversion failures MUST retain a concise diagnostic from the LibreOffice process when process output is available.

#### Scenario: LibreOffice exits without a usable PDF

- **WHEN** LibreOffice exits with a non-zero status or exits without producing the expected PDF file
- **THEN** the thrown conversion error includes the existing Word conversion failure category
- **AND** the thrown conversion error includes a bounded diagnostic from LibreOffice stdout or stderr when one was captured
- **AND** the uploaded document can still be marked failed by the existing batch processing flow
