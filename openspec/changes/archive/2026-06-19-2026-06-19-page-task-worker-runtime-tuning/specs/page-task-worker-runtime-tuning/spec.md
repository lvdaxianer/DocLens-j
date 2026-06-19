## ADDED Requirements

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

When no explicit OCR request thread-pool override is configured, the local OCR request executor MUST default to the maximum enabled bootstrap-node concurrency so local thread pools do not silently cap configured node throughput.

#### Scenario: Bootstrap node allows ten concurrent OCR requests

- **GIVEN** an enabled bootstrap OCR node has `max-concurrency` set to 10
- **AND** no explicit OCR request thread-pool override is configured
- **WHEN** OCR request infrastructure is auto-configured
- **THEN** the OCR request thread pool can execute up to 10 concurrent requests
