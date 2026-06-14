## Context

DocLens-j already exposes `/api/v1/health`, but that endpoint is a
generic compatibility API. Other services need a dedicated heartbeat
check with a stable contract and no dependency on OCR or LLM state.

## Goals / Non-Goals

**Goals:**
- Provide a dedicated heartbeat endpoint for external probes.
- Keep the endpoint lightweight and safe to call frequently.
- Avoid dependency checks so the endpoint reflects process liveness,
  not downstream service health.

**Non-Goals:**
- Do not replace or remove the existing `/api/v1/health` endpoint.
- Do not add readiness or dependency status logic.
- Do not add authentication or configuration for this first version.

## Decisions

- Use `GET /api/v1/heartbeat` as the external probe path.
- Return a fixed payload with `status`, `service`, and `timestamp`.
- Format `timestamp` as an ISO-8601 string with timezone offset.
- Keep the implementation in the existing system controller area so the
  API surface stays easy to find.
- Cover the new endpoint with contract tests to lock down the shape.

## Risks / Trade-offs

- This endpoint duplicates a small amount of information already present
  in `/api/v1/health`, but the explicit path is clearer for callers.
- Because the endpoint intentionally ignores downstream health, callers
  must not treat it as readiness or full system health.
