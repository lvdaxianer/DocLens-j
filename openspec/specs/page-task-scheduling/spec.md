# page-task-scheduling Specification

## Purpose
TBD - created by archiving change 2026-06-20-ocr-total-concurrency-recovery. Update Purpose after archive.
## Requirements
### Requirement: Queued page task scans are fair across documents

The page-task repository MUST return queued page tasks in a way that distributes
limited worker scans across multiple documents before giving all slots to a
single large document.

#### Scenario: Large PDF does not monopolize a recovery scan

- **GIVEN** one document has many older queued page tasks
- **AND** two other documents also have queued page tasks
- **WHEN** the worker asks for 3 queued page tasks
- **THEN** the returned tasks include one task from each document
- **AND** the large document does not receive all 3 slots

#### Scenario: Fair scans preserve document page order

- **GIVEN** a document has queued page tasks for page 1 and page 2
- **WHEN** fair queued task selection returns both pages for that document
- **THEN** page 1 appears before page 2 for that document

