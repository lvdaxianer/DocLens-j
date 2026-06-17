# dashboard-batch-detail Specification

## Purpose
Define the Dashboard batch detail information that operators need when diagnosing batch intake, OCR processing, callback delivery, and third-party integration flows.
## Requirements
### Requirement: Batch intake information is visible
The Dashboard batch detail view MUST show the batch creation intake fields needed to diagnose third-party upload and callback flows.

#### Scenario: Operator inspects intake fields
- **WHEN** an operator opens a Dashboard batch detail page for a batch created with `callback_url`, `idempotency_key`, and `metadata`
- **THEN** the page displays the callback URL
- **AND** the page displays the idempotency key
- **AND** the page displays the metadata as readable JSON

#### Scenario: Intake fields are absent
- **WHEN** an operator opens a Dashboard batch detail page for a batch without callback URL, idempotency key, or metadata
- **THEN** the page displays stable empty placeholders for the missing callback URL and idempotency key
- **AND** the metadata area displays an empty meta state instead of disappearing

### Requirement: Batch detail API exposes intake fields
The Dashboard batch detail API MUST expose the batch creation intake fields on the `batch` object.

#### Scenario: Detail response includes intake fields
- **WHEN** a client requests Dashboard batch detail
- **THEN** the response `batch` object contains `callback_url`
- **AND** the response `batch` object contains `idempotency_key`
- **AND** the response `batch` object contains `metadata`
