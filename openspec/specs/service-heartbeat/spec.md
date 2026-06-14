# service-heartbeat Specification

## Purpose
Provide a stable, lightweight heartbeat endpoint for other services to
probe DocLens-j liveness without depending on OCR, LLM, or database
health.
## Requirements
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

### Requirement: Existing health endpoint remains available
DocLens-j MUST continue to expose the existing `/api/v1/health`
endpoint for backward compatibility.

#### Scenario: Legacy health request succeeds
- **WHEN** an external caller sends `GET /api/v1/health`
- **THEN** the API returns HTTP 200
- **AND** the response includes `status` equal to `ok`
