## Why

Users can configure and enable LLM providers, but OCR results may still show
`LLM 未应用` because the runtime selector excludes configurations whose latest
health flag is false.

That makes health checks act like a processing-time gate. The intended behavior is
different: once an LLM config is complete and enabled, DocLens should attempt
Markdown formatting. If the provider call fails, the result should show fallback or
failure details instead of pretending no LLM was applied.

## What Changes

Allow configured and enabled LLM Markdown configs to be selected for post-processing
even when the last health check marked them unhealthy.

## Impact

The health flag remains visible for diagnostics and operations, but it no longer
prevents Markdown post-processing attempts. Disabled or incomplete configs remain
excluded.
