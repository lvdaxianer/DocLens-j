# callback-delivery Specification

## Purpose
Define how DocLens persists, delivers, retries, and exposes OCR completion
callbacks for uploaded batches that provide a `callback_url`.
## Requirements
### Requirement: Durable Callback Delivery

The system SHALL create a persistent callback job for each completed OCR
document that has a non-empty `callback_url`.

#### Scenario: OCR completes with callback URL

- **WHEN** a document reaches the completed OCR state and the batch contains a
  callback URL
- **THEN** the system creates a callback job that stores the callback URL,
  payload body, retry count, and delivery status

### Requirement: Callback Success And Failure

The system SHALL deliver callback jobs by sending HTTP POST requests and SHALL
record whether each attempt succeeds or fails.

#### Scenario: Callback returns success

- **WHEN** the callback endpoint returns a 2xx response
- **THEN** the job is marked successful and no further retry is scheduled

#### Scenario: Callback returns failure

- **WHEN** the callback endpoint returns a non-2xx response, times out, or
  raises a network or unexpected exception
- **THEN** the job is marked failed for that attempt and the system records a
  failure reason and failure detail

### Requirement: Callback Retry Policy

The system SHALL retry failed callback jobs with bounded attempts and backoff.

#### Scenario: Callback fails before terminal limit

- **WHEN** a callback attempt fails and the retry count is still below the
  configured limit
- **THEN** the job remains retryable, the retry count increases, and the next
  retry time is scheduled

#### Scenario: Callback reaches terminal failure

- **WHEN** a callback attempt fails after the retry limit is reached
- **THEN** the job is marked terminal failed and both the failure reason and
  failure detail remain available for inspection
