## ADDED Requirements

### Requirement: Batch route panel distinguishes running allocation and final attribution

The Dashboard batch OCR route panel MUST show active OCR allocation and final OCR attribution as separate concepts.

#### Scenario: Current document is still running OCR

- **WHEN** the current document has running OCR allocations but no final OCR hit nodes yet
- **THEN** the route panel displays the current running node allocation
- **AND** the route panel labels final attribution as unavailable until successful OCR rows exist
- **AND** the route panel displays node inflight and max concurrency values when available

#### Scenario: Current document has completed OCR

- **WHEN** the current document has final OCR hit nodes
- **THEN** the route panel displays the final node attribution separately from any batch-level dispatch history

### Requirement: Batch route panel shows batch concurrency snapshot

The Dashboard batch OCR route panel MUST show a batch-level concurrency snapshot derived from OCR runtime capacity.

#### Scenario: OCR runtime capacity is available

- **WHEN** the batch detail response includes model and node runtime capacity
- **THEN** the route panel displays model-level `inflight / max concurrency`
- **AND** the route panel displays node-level `inflight / max concurrency`
