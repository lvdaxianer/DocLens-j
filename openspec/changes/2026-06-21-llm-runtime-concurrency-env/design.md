## Context

The current runtime Markdown processor builds its credential resolver from
`ConfigurableMarkdownPostProcessorOptions.environmentValues()`. In production,
`DocLensLlmMarkdownAutoConfiguration` passes `null`, and the options record
normalizes that to `Map.of()`. The resolver then only checks the empty map and
throws `credential environment variable ... is not configured`.

The LLM config health checker uses a different path, so it can report the
configuration as healthy while actual document post-processing still fails.

For chunk concurrency, `ChunkedMarkdownPostProcessor` already submits all chunks
to a shared executor. The observed serialization comes later from
`LlmConfigRateLimiter`, which stores one `lastStartMillis` value per config. That
single value spaces every request start for the config, even when
`maxConcurrency` allows multiple simultaneous requests.

## Decisions

- Treat a missing environment map as "use `System.getenv()`".
- Keep explicit maps for tests and deterministic callers.
- Replace the config-global interval timestamp with per-slot timestamp tracking.
- Acquire a concurrency slot first, then apply the interval only for that slot.
- Rebuild limiter state when `maxConcurrency` or `requestIntervalMillis` changes
  for the same config ID.
- Keep the existing synchronous `MarkdownPostProcessor` contract.
- Update user-facing missing credential copy to say the saved result could come
  from a missing or previously missing backend environment variable, and point
  operators to re-run/retry after confirming the config.

## Data Flow

1. Spring creates `ConfigurableMarkdownPostProcessorOptions` without an explicit
   environment map.
2. The options object leaves the map absent instead of converting it into an
   empty source.
3. `EnvironmentCredentialResolver` reads the process environment.
4. Chunk tasks enter the shared executor and acquire a slot for their selected
   LLM config.
5. Each slot independently applies the configured request interval before
   starting the provider call.
6. The coordinator joins chunk results in original order as before.

## Risks / Trade-offs

- Per-slot interval semantics can increase provider traffic compared with the
  previous accidental serialization. That matches the operator's explicit
  concurrency setting but makes the setting more important.
- Old saved document results can still contain the previous credential failure.
  The UI copy must avoid implying the current process environment is still
  missing without a fresh retry.
- The upload routing selector currently applies a model-bound default after
  loading OCR models. That helper should be removed or narrowed so it does not
  override the route default.
- A favicon can be served from Vite's public assets and referenced in
  `index.html`; no runtime JavaScript is needed.

## Testing

- Add backend tests proving the production options path resolves credentials
  from `System.getenv()` when no explicit environment map is supplied.
- Add backend tests proving explicit environment maps still work for tests.
- Add limiter tests proving two requests can start without a full interval delay
  when `maxConcurrency=2`.
- Add limiter tests proving a reused config ID picks up changed concurrency.
- Add frontend tests for the less misleading credential failure copy.
- Add frontend tests proving the OCR routing selector keeps the global route
  after model loading.
- Add a lightweight index/favicon assertion or build verification for the
  favicon link.
