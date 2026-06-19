## 1. Apply LLM Markdown whenever configured

- [x] 1.1 Add a failing backend regression test proving the legacy orchestration flag does not skip configured LLM Markdown formatting.
- [x] 1.2 Remove the processing-time bypass so Markdown post-processing always delegates to the configured processor.
- [x] 1.3 Run focused and broader backend processing tests, then validate the OpenSpec change.
