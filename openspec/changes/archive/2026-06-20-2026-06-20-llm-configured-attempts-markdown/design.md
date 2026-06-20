## Context

The current selector filters configs with:

```text
enabled + configured + healthy
```

This means a transient health-check failure can cause completed OCR results to skip
LLM Markdown entirely and show `LLM 未应用`, even though the user has configured and
enabled LLM providers.

## Goals / Non-Goals

**Goals:**
- Treat `enabled && configured` as sufficient for attempting Markdown formatting.
- Keep disabled and incomplete configs excluded.
- Preserve health information as observability only.
- Keep existing fallback behavior when an actual LLM request fails.

**Non-Goals:**
- Do not change provider health checks.
- Do not change prompts, chunking, retry count, or fallback text.
- Do not change UI copy in this task.

## Decision

Change `LlmConfigSelector` so `healthy` is no longer part of selection eligibility.
Configs are still ordered and round-robined by priority and ID. The actual provider
call remains responsible for success/failure. If the call fails, existing retry and
fallback paths persist warnings and error details.

## Data Flow

1. User configures and enables an LLM provider.
2. OCR result building asks the selector for a Markdown config.
3. Selector returns a configured and enabled config regardless of last health flag.
4. Markdown post-processor attempts the LLM call.
5. Success stores `llm_markdown_applied=true`; failure stores fallback warning and
   error detail.

## Risks / Trade-offs

- A known-unhealthy provider may still be attempted and fail. That is acceptable
  because the user explicitly enabled it, and failure now surfaces through the
  result fallback path instead of silently skipping LLM.
- Health remains useful for dashboards and troubleshooting, but not as a hard gate.

## Testing

- Add a RED test proving an enabled configured but unhealthy config is still selected.
- Update existing selector tests whose names/expectations encoded health as a hard
  gate.
- Run focused selector tests and the relevant Markdown processing test slice.
