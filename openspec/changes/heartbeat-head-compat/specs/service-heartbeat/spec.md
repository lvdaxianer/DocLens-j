## MODIFIED Requirements

### Requirement: External heartbeat endpoint
DocLens-j MUST expose a lightweight heartbeat endpoint for other
services to probe service liveness.

#### Scenario: Heartbeat request succeeds
- **WHEN** an external caller sends `GET /api/v1/heartbeat`
- **THEN** the API returns HTTP 200
- **AND** the response includes `status` equal to `ok`
- **AND** the response includes `service` equal to `doclens-j`
- **AND** the response includes a `timestamp` string
- **AND** the endpoint does not require OCR, LLM, or database checks to
  succeed

#### Scenario: Heartbeat HEAD request succeeds
- **WHEN** an external caller sends `HEAD /api/v1/heartbeat`
- **THEN** the API returns HTTP 200
- **AND** the response has no body
- **AND** the endpoint does not require OCR, LLM, or database checks to
  succeed
