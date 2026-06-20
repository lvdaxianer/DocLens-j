## ADDED Requirements

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
