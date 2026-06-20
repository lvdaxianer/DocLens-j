# page-task-worker-runtime-tuning Specification

## Purpose
Define the runtime behavior that keeps page-level OCR worker locks, local worker
parallelism, and OCR request executor capacity aligned with configured OCR node
timeouts and concurrency.
## Requirements
### Requirement: Page task locks cover configured OCR request duration

The page-task worker MUST use an effective lock duration that is at least the configured OCR request timeout plus a safety buffer unless an operator explicitly configures a larger lock.

#### Scenario: Slow OCR requests are not recovered before their timeout window

- **GIVEN** PaddleOCR request timeout is configured to 600 seconds
- **WHEN** the page-task worker creates claim requests
- **THEN** the page-task lock duration is at least 630 seconds
- **AND** the stale-task recovery worker does not reset normally running OCR requests after 60 seconds

### Requirement: Page task worker concurrency is configurable

The page-task worker MUST expose configurable batch size, executor pool size, queue capacity, recovery limit, and scheduling interval.

#### Scenario: Operator configures page task worker concurrency

- **GIVEN** an operator configures page-task worker pool size and batch size to 10
- **WHEN** the page-task worker is auto-configured
- **THEN** it uses the configured pool size
- **AND** it claims up to the configured batch size per scan

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

