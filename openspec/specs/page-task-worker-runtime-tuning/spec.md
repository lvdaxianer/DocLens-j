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
concurrency of dashboard-persisted OCR nodes that are enabled, participate in
global routing, and are healthy. When no dashboard-persisted OCR node qualifies,
the defaults MUST fall back to the aggregate concurrency of enabled bootstrap
OCR nodes that participate in global routing. Explicit OCR request thread-pool
and page-task worker overrides MUST be respected.

#### Scenario: Dashboard nodes override bootstrap concurrency

- **GIVEN** the dashboard has three persisted OCR nodes that are enabled,
  participate in global routing, and are `UP`
- **AND** their `maxConcurrency` values are 10, 4, and 10
- **AND** the bootstrap configuration contains one enabled global OCR node with
  `max-concurrency` set to 10
- **AND** no explicit page-task worker or OCR request thread-pool override is
  configured
- **WHEN** OCR request infrastructure is auto-configured
- **THEN** the OCR request thread pool can execute up to 24 concurrent requests
- **AND** the page-task worker can claim and execute up to 24 page tasks per scan

#### Scenario: Unhealthy dashboard nodes do not increase local concurrency

- **GIVEN** the dashboard has two persisted OCR nodes
- **AND** one node is enabled, participates in global routing, and is `UP` with
  `maxConcurrency` 10
- **AND** the other node is enabled, participates in global routing, and is
  `DOWN` with `maxConcurrency` 10
- **WHEN** OCR request infrastructure is auto-configured
- **THEN** the derived default local OCR concurrency is 10

#### Scenario: Bootstrap nodes are used when no dashboard node qualifies

- **GIVEN** no dashboard-persisted OCR node is enabled, participates in global
  routing, and is `UP`
- **AND** three enabled bootstrap OCR nodes participate in global routing
- **AND** each bootstrap node has `max-concurrency` set to 10
- **WHEN** OCR request infrastructure is auto-configured
- **THEN** the OCR request thread pool can execute up to 30 concurrent requests
- **AND** the page-task worker can claim and execute up to 30 page tasks per scan

### Requirement: OCR concurrency capacity is observable

Dashboard OCR resource metrics MUST expose both active runtime usage and configured
capacity for the OCR request executor and the page-task OCR executor.

#### Scenario: OCR executors are configured from node capacity

- **GIVEN** effective OCR request executor maximum size is 30
- **AND** effective page-task worker pool size is 30
- **WHEN** the dashboard fetches OCR resource metrics
- **THEN** the OCR request thread-pool metrics include `maximum_pool_size` 30
- **AND** the page-task worker metrics include `pool_size` 30
- **AND** the page-task worker metrics include the effective `batch_size`

### Requirement: Same-node page tasks can run in parallel

The page-task worker MUST be able to submit multiple pages for the same document
to the same OCR node concurrently when the local worker pool, OCR request pool,
and node max concurrency all have capacity.

#### Scenario: Two pages use one node concurrently

- **GIVEN** one OCR node is eligible and has max concurrency 10
- **AND** the page-task worker pool size and batch size are at least 2
- **AND** one document has two queued page tasks
- **WHEN** the page-task worker runs one scan
- **THEN** both page OCR calls can be in flight at the same time
- **AND** neither page waits for the other page to complete before its OCR call starts

