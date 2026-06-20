## ADDED Requirements

### Requirement: Batch detail exposes OCR runtime capacity

The Dashboard batch detail API MUST expose an OCR runtime capacity snapshot that helps operators understand active OCR concurrency for the batch route.

#### Scenario: Batch is routed to an OCR model

- **WHEN** a client requests Dashboard batch detail for a batch routed to `paddle_ocr`
- **THEN** the response contains `ocr_runtime_capacity`
- **AND** the capacity snapshot contains the model key and model display name
- **AND** the capacity snapshot contains the model current inflight image count
- **AND** the capacity snapshot contains the model max concurrency capacity
- **AND** the capacity snapshot contains node rows with each node's inflight image count and max concurrency

#### Scenario: Batch has active OCR allocation for the current document

- **WHEN** a document in the batch has OCR images currently dispatched to nodes
- **THEN** the response contains `ocr_running_hit_nodes_by_document`
- **AND** the current document can be matched to its running node allocations before final OCR success rows exist
