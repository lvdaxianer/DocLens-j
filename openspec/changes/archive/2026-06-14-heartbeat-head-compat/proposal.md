## Why

Some callers only need a liveness probe and prefer `HEAD` over `GET`
because they do not want response bodies. The existing heartbeat
endpoint already serves external probes, so adding `HEAD` support keeps
the interface compatible without breaking current `GET` callers.

## What Changes

Add `HEAD /api/v1/heartbeat` as a bodyless compatibility variant of the
existing heartbeat endpoint. `GET /api/v1/heartbeat` remains available
and unchanged.

## Capabilities

### New Capabilities
- `service-heartbeat-head`: supports bodyless heartbeat probes.

### Modified Capabilities
- `service-heartbeat`: adds HEAD compatibility without changing the GET
  payload contract.

## Impact

Backend controller code, heartbeat contract tests, and the service
heartbeat specification.
