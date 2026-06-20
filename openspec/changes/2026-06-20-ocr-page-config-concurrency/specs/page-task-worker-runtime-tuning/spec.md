# page-task-worker-runtime-tuning Specification Delta

## Modified Requirements

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
