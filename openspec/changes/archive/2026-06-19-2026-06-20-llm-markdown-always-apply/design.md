## Context

`DocumentMarkdownPostProcessingService` currently returns OCR text directly when `document.llmOrchestrated()` is true. The UI then shows `LLM 未应用` because the stored result contains `llm_markdown_applied=false`. The business rule is different: DocLens is the orchestration owner, so configured LLM Markdown formatting should run for all OCR outputs.

## Decisions

- Treat `llmOrchestrated` as non-authoritative for Markdown post-processing.
- Keep Markdown post-processing selection centralized in `MarkdownPostProcessor`.
- Preserve fallback behavior so missing config and LLM failure remain distinguishable.
- Avoid schema changes in this correction; removing the persisted flag can be handled later as cleanup.

## Data Flow

1. OCR/text extraction produces `DocumentTextExtractionResult`.
2. `DocumentMarkdownPostProcessingService` always builds a Markdown request.
3. The configured `MarkdownPostProcessor` decides whether a usable LLM config exists.
4. Successful LLM formatting stores Markdown and `llm_markdown_applied=true`.
5. Missing config or failed LLM keeps OCR text and stores the existing warning/failure metadata.

## Testing

- Add a backend regression test where a document is created with `llmOrchestrated=true`.
- Use a fixed Markdown post-processor and assert the final OCR result stores Markdown with `llm_markdown_applied=true`.
- Run the focused LLM Markdown test slice and a broader processing slice.
