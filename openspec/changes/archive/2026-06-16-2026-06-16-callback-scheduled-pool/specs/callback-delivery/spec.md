## MODIFIED Requirements

### Requirement: Callback Retry Policy
The system SHALL retry failed callback jobs with bounded attempts and delayed backoff using a callback delivery scheduled pool whose core thread count is 3.

#### Scenario: Callback fails before terminal limit
- **WHEN** a callback attempt fails and the retry count is still below the configured limit
- **THEN** the job remains retryable, the retry count increases, and the next retry time is scheduled
- **AND** the next delivery attempt does not run before the scheduled retry time

#### Scenario: Callback reaches terminal failure
- **WHEN** a callback attempt fails after the retry limit is reached
- **THEN** the job is marked terminal failed and both the failure reason and failure detail remain available for inspection

#### Scenario: Callback delivery pool avoids self blocking
- **WHEN** scheduled callback delivery scans pending jobs
- **THEN** callback delivery work is executed by a scheduled pool with core thread count 3
- **AND** the scanner does not synchronously wait on child tasks submitted to the same single worker thread

#### Scenario: Callback succeeds without retry
- **WHEN** a callback endpoint returns a 2xx response on the first attempt
- **THEN** the job is marked successful
- **AND** no retry time is scheduled
- **AND** no further automatic delivery attempt is made for that job
