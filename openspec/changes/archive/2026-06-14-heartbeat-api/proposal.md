## Why

Other services need a stable, low-cost heartbeat endpoint to probe
DocLens-j liveness without depending on OCR, LLM, or database health.
The existing `/api/v1/health` endpoint is generic and should keep its
current compatibility role.

## What Changes

Add a dedicated `GET /api/v1/heartbeat` endpoint that returns a small,
predictable payload for external heartbeat checks.

The new endpoint reports only service liveness metadata and does not
trigger downstream dependency checks.

## Capabilities

### New Capabilities
- `service-heartbeat`: exposes a stable heartbeat endpoint for external
  service probes.

### Modified Capabilities

## Impact

Backend controller code, contract tests, and API documentation for
service liveness.
