## Context

The DocLens-j heartbeat endpoint already exposes `GET /api/v1/heartbeat`
for external probes. Some integrations only need a status code and do
not want a JSON body, so they should be able to call `HEAD` instead.

## Goals / Non-Goals

**Goals:**
- Support `HEAD /api/v1/heartbeat` for liveness probes.
- Keep `GET /api/v1/heartbeat` unchanged for existing callers.
- Return HTTP 200 with no response body for `HEAD`.

**Non-Goals:**
- Do not change the existing `/api/v1/health` contract.
- Do not add authentication or dependency checks.
- Do not change the response payload for `GET`.

## Decisions

- Implement `HEAD` alongside the existing `GET` heartbeat endpoint in
  the same controller.
- Keep the `GET` payload contract stable with `status`, `service`, and
  `timestamp`.
- Add contract coverage for both the bodyless `HEAD` probe and the
  existing `GET` probe.

## Risks / Trade-offs

- `HEAD` and `GET` share the same route, so controller mapping must stay
  precise to avoid ambiguous bindings.
- Callers relying on the body for `HEAD` will get nothing by design;
  they should use `GET` if they need metadata.
