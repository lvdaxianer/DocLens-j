## Why

Configured LLM Markdown post-processing currently can be skipped when a document is marked as already orchestrated. That flag does not match the product model: DocLens itself owns OCR and Markdown formatting, so a configured LLM should format every supported OCR result regardless of upload format or legacy orchestration metadata.

## What Changes

- Remove the processing-time bypass that skips Markdown post-processing for `llmOrchestrated=true` documents.
- Keep the existing no-config and failure behavior: no available config returns OCR text with the no-config warning; downstream LLM failures retry and then fall back to OCR.
- Add a regression test proving a document carrying the legacy orchestration flag still uses the configured Markdown post-processor.

## Impact

Existing uploads that carry `llmOrchestrated=true` will now receive LLM Markdown formatting when a usable LLM config exists. This can increase LLM usage but matches the requirement that configured LLM formatting applies to all formats.
