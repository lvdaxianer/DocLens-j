## ADDED Requirements

### Requirement: Upload chunk strategy selection SHALL be available during file upload

The dashboard upload form SHALL let the user choose a chunking preset before the
batch is submitted.

#### Scenario: User opens the upload form

- **WHEN** the user visits the upload page
- **THEN** the form shows the chunking presets `GENERAL`, `NEWS`, `TECHNICAL`,
  and `ACADEMIC`
- **AND** `GENERAL` is selected by default
- **AND** the preset defaults are `GENERAL` = `400/80`, `NEWS` = `300/60`,
  `TECHNICAL` = `500/100`, and `ACADEMIC` = `800/150` for
  `chunk_size/overlap`
- **AND** no raw numeric chunk size fields are shown in the first release

### Requirement: The selected chunk strategy SHALL flow through the upload request

The multipart upload request SHALL include the selected chunk strategy preset so
that the server can resolve preset-specific chunking defaults.

#### Scenario: User submits the upload form with a preset

- **WHEN** the user selects `TECHNICAL` and submits files
- **THEN** the multipart payload includes `chunkStrategy=TECHNICAL`
- **AND** the server request mapper preserves that field on the batch command

### Requirement: Backend chunking defaults SHALL depend on the upload preset

The backend SHALL translate the selected preset into chunking defaults before it
creates the Markdown chunk plan.

#### Scenario: Different presets resolve to different window sizes

- **WHEN** the backend resolves `NEWS`, `TECHNICAL`, and `ACADEMIC`
- **THEN** the resulting chunking defaults are not identical across all presets
- **AND** the general preset remains the fallback when the request omits the field
- **AND** the sliding-window algorithm itself remains unchanged
