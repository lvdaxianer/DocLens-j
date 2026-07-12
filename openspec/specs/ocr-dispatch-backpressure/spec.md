# ocr-dispatch-backpressure Specification

## Purpose
Define bounded OCR dispatch queue behavior and dispatch wait timeout guarantees
for saturated OCR routing.
## Requirements
### Requirement: OCR dispatch queue applies bounded backpressure

The OCR pending dispatch queue MUST have a finite capacity and MUST reject new
pending OCR requests when the queue is full.

#### Scenario: Pending queue rejects overflow

- **GIVEN** the OCR pending dispatch queue capacity is 1
- **AND** one OCR request is already waiting in the queue
- **WHEN** another OCR request needs to wait for a node slot
- **THEN** the second request is rejected with a stable dispatch queue full error
- **AND** the request is not silently retained in memory

### Requirement: OCR dispatch waiting has a timeout

OCR routing MUST stop waiting for a queued dispatch after the configured timeout
and MUST surface a stable route execution failure.

#### Scenario: Queued dispatch times out

- **GIVEN** an OCR request has entered the pending dispatch queue
- **AND** no matching node slot becomes available before the configured timeout
- **WHEN** OCR routing waits for dispatch
- **THEN** routing fails with a stable dispatch timeout error
- **AND** the caller can persist a recoverable page-task failure
