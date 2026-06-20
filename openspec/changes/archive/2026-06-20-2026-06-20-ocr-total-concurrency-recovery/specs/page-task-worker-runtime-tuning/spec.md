## MODIFIED Requirements

### Requirement: Default local OCR concurrency follows configured node capacity

The local OCR request executor and page-task worker MUST default to the aggregate
concurrency of enabled bootstrap OCR nodes that participate in global routing
when no explicit OCR request thread-pool or page-task worker override is
configured.

#### Scenario: Multiple OCR nodes allow thirty concurrent OCR requests

- **GIVEN** three enabled bootstrap OCR nodes participate in global routing
- **AND** each node has `max-concurrency` set to 10
- **AND** no explicit page-task worker or OCR request thread-pool override is configured
- **WHEN** OCR request infrastructure is auto-configured
- **THEN** the OCR request thread pool can execute up to 30 concurrent requests
- **AND** the page-task worker can claim and execute up to 30 page tasks per scan

#### Scenario: Explicit page-task worker override is respected

- **GIVEN** three enabled bootstrap OCR nodes provide 30 aggregate slots
- **AND** an operator explicitly configures page-task worker pool size and batch size to 12
- **WHEN** the page-task worker is auto-configured
- **THEN** it uses the configured pool size of 12
- **AND** it claims up to the configured batch size of 12 per scan
