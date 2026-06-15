## MODIFIED Requirements

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
