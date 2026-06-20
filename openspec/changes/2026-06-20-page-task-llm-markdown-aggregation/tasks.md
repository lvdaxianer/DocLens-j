## 1. Page-task aggregation LLM Markdown

- [ ] 1.1 Add RED tests proving page-task aggregation applies Markdown
  post-processing, persists `llm_markdown_applied=true`, and carries chunk
  metadata into raw output.
- [ ] 1.2 Add RED test proving page-task aggregation LLM failure falls back to
  OCR text with the existing fallback warning and error message.
- [ ] 1.3 Inject `MarkdownPostProcessor` into page-task aggregation dependencies
  and call `DocumentMarkdownPostProcessingService` before saving the final
  `OcrResult`.
- [ ] 1.4 Verify focused page-task aggregation tests and the relevant broader
  processing test slice.
