# dashboard-result-rendering Specification

## Purpose
TBD - created by archiving change 2026-06-20-markdown-table-upload-guard. Update Purpose after archive.
## Requirements
### Requirement: Markdown tables render as tables

The dashboard result drawer MUST render valid pipe-table Markdown in document
result text as an HTML table instead of showing the raw pipe syntax.

#### Scenario: Result contains a pipe table

- **GIVEN** a document result final text contains a Markdown table with a header
  row, separator row, and at least one body row
- **WHEN** the user opens the result drawer
- **THEN** the table is displayed as a table with header and body cells
- **AND** raw pipe separator syntax is not shown as paragraph text

### Requirement: Markdown table rendering remains safe

Markdown table rendering MUST escape untrusted cell content before it reaches the
HTML preview.

#### Scenario: Table cell contains script text

- **GIVEN** a document result final text contains script-like text inside a
  Markdown table cell
- **WHEN** the preview HTML is rendered
- **THEN** the script is escaped as text
- **AND** no executable `<script>` element is present in the preview HTML

