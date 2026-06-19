## 1. Surface chunk count in batch detail document rows

- [x] 1.1 Add a failing backend test that proves batch detail document rows include `llm_chunk_count` for chunked Markdown documents.
- [x] 1.2 Update the dashboard query assembler and document row read model so batch detail includes a stable chunk count field derived from OCR result metadata.
- [x] 1.3 Add a dashboard test that proves the batch detail page renders the chunk count next to the document stage or track summary.
- [x] 1.4 Re-run the focused backend and dashboard tests to confirm the chunk count is visible without changing the existing stage labels or progress logic.

## 2. Expose LLM Markdown chunk executor activity

- [x] 2.1 Add a failing backend test that proves OCR resource metrics include the LLM Markdown chunk executor queue and active counts.
- [x] 2.2 Extend the dashboard OCR metrics provider and resource card UI so the chunk executor appears as a dedicated metric card.
- [x] 2.3 Add or adjust the dashboard test that covers OCR resource cards so the new chunk executor metric is rendered.
- [x] 2.4 Re-run the focused backend metrics test and the dashboard build / test slice to confirm the new card does not disturb the existing OCR resource layout.
