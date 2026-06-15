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
