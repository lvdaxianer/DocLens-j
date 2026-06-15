## Context

The OCR pipeline already stores batches, documents, results, and completion
events. The callback contract body is assembled from the completed document
state, but nothing in the codebase currently sends that body to the uploaded
`callback_url`.

## Goals / Non-Goals

**Goals:**
- Deliver the completed callback payload to the uploaded `callback_url`.
- Persist callback jobs so delivery survives process restarts.
- Record callback success, retrying, and terminal failure states.
- Store both a machine-readable failure reason and a readable failure detail
  for every failed attempt.
- Retry failed deliveries with bounded attempts and backoff.

**Non-Goals:**
- Do not change OCR recognition, page aggregation, or document result building.
- Do not add synchronous callback delivery in the request thread.
- Do not hide callback failures behind successful OCR completion.

## Decisions

- Create one callback job per completed document that has a non-empty
  `callback_url`.
- Use an internal callback worker that scans pending jobs and sends HTTP POST
  requests.
- Treat any 2xx response as success.
- Treat timeouts, network failures, non-2xx responses, and unexpected
  exceptions as failures.
- Persist `failure_reason` and `failure_detail` on every failed attempt; the
  terminal failure state must always include both fields.
- Use bounded retry attempts with backoff and keep the failure reason stable
  enough for downstream diagnosis.

## Risks / Trade-offs

- Callback retries add operational traffic and storage writes.
- Persisting the callback payload increases the amount of stored data, but it
  makes retries and auditability deterministic.
- The callback worker must avoid duplicate dispatch when jobs are picked up by
  more than one run.
