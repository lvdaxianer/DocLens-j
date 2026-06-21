## Why

Operators configure LLM Markdown credentials and concurrency from the Dashboard.
Two runtime gaps currently make that configuration misleading:

- The Spring auto-configuration passes a null environment map into the runtime
  Markdown processor. The options object converts that null into an empty map,
  so credential resolution can fail even when the backend process actually has
  `DOCLENS_LLM_KEY`.
- The per-config request interval is enforced as one global start gate. With
  `max_concurrency=10` and `request_interval_millis=1000`, two chunks selected
  for the same LLM config start about one second apart instead of using the
  available concurrency slots.

These gaps make successful health checks disagree with document processing and
make chunk execution look serial even when the operator set a higher concurrency.

## What Changes

- Use the backend process environment as the default credential source when no
  test-specific environment map is supplied.
- Preserve testability by allowing explicit environment maps to override the
  process environment in unit tests.
- Change LLM request interval governance from config-global serialization to
  per-concurrency-slot spacing.
- Refresh the per-config limiter state when the saved concurrency or interval
  settings change.
- Improve Dashboard wording so credential failures do not overstate that the
  service process lacks the environment variable when the stored result may be
  stale.
- Add a Dashboard favicon so browser tabs no longer show the generic browser
  icon.
- Keep the upload form's default OCR route on global load balancing instead of
  silently switching to a specific OCR after model options load.

## Impact

- Documents created after the fix can use `DOCLENS_LLM_KEY` from the restarted
  backend process without requiring a custom environment map.
- A two-chunk document can run both chunk LLM calls concurrently when
  `max_concurrency` is at least two and provider capacity allows it.
- `request_interval_millis` still protects each concurrency slot from overly
  frequent repeated starts, but it no longer defeats the configured concurrency.
- New upload batches default to global OCR load balancing unless the operator
  explicitly selects a model or node route.
- Existing OCR extraction, batch recovery, chunk checkpointing, and saved result
  schemas remain unchanged.
