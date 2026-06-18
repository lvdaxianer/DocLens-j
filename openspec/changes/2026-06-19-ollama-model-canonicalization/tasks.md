## 1. Ollama model key canonicalization

- [ ] 1.1 Add failing tests that prove `/api/v1/ocr-models` returns a canonical `ollama` model entry with a readable `name`, while `ollama_deepseek_ocr` remains accepted as a compatibility alias.
- [ ] 1.2 Update the backend model definition and response serialization so the canonical key is `ollama`, the display name is `Ollama`, and the API exposes `name` explicitly.
- [ ] 1.3 Update the dashboard OCR model selector and form coverage so Ollama shows as `Ollama` while the provider model input remains free-form.
- [ ] 1.4 Re-run the focused backend contract test and dashboard component test, then mark the task complete and commit the change with a Chinese Conventional Commit message.

