# callback-delivery Specification

## Purpose
Define how DocLens persists, delivers, retries, and exposes OCR completion
callbacks for uploaded batches that provide a `callback_url`.
## Requirements
### Requirement: Durable Callback Delivery

The system SHALL create a persistent callback job for each completed OCR document that has a non-empty `callback_url`.

#### Scenario: OCR completes with callback URL

- **WHEN** a document reaches the completed OCR state and the batch contains a callback URL
- **THEN** the system creates a callback job that stores the callback URL, event ID, batch ID, document ID, payload body, retry count, and delivery status
- **AND** the callback job payload is the `callback_body` recorded on the `document.completed` event

#### Scenario: OCR completes without callback URL

- **WHEN** a document reaches the completed OCR state and the batch has no callback URL
- **THEN** the system does not create a callback job for that completed document

### Requirement: Callback Success And Failure

The system SHALL deliver callback jobs by sending HTTP POST requests and SHALL
record whether each attempt succeeds or fails. The Dashboard batch detail page
SHALL display recorded callback outcomes for the batch.

#### Scenario: Callback returns success

- **WHEN** the callback endpoint returns a 2xx response
- **THEN** the job is marked successful and no further retry is scheduled

#### Scenario: Callback returns failure

- **WHEN** the callback endpoint returns a non-2xx response, times out, or
  raises a network or unexpected exception
- **THEN** the job is marked failed for that attempt and the system records a
  failure reason and failure detail

#### Scenario: Operator inspects callback failure on batch detail page

- **WHEN** a batch detail response contains a failed callback job
- **THEN** the Dashboard batch detail page displays the callback status,
  retry count, failure reason, and failure detail

#### Scenario: Operator manually retries failed callback on batch detail page

- **WHEN** a batch detail response contains a failed callback job
- **THEN** the Dashboard batch detail page provides a retry action for that
  failed callback job
- **AND** clicking the retry action triggers an immediate callback delivery for
  that callback job
- **AND** the page refreshes the batch detail data after the retry finishes

#### Scenario: Manual callback retry records latest failure reason

- **WHEN** a manually retried callback delivery fails again
- **THEN** the system records the latest failure reason and failure detail using
  the same callback failure contract as scheduled delivery
- **AND** the Dashboard batch detail page displays that recorded failure reason
  and failure detail after refresh

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

