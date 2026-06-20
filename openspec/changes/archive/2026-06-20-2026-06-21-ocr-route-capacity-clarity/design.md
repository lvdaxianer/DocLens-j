## Context

`createDefaultUploadOcrRouting()` currently returns `GLOBAL_LOAD_BALANCE`, so upload requests can be dispatched to any healthy enabled node with `participate_global=true`. `OcrRoutingService.confirmedAffinityPolicy()` then binds the document to whichever model wins the first page dispatch. The built-in OCR registry includes `ollama` and its default provider model is `deepseek-ocr:latest`; legacy `ollama_deepseek_ocr` rows are normalized to `ollama`.

The OCR resources page already receives each node's `max_concurrency`, `inflight_images`, and `participate_global` fields, but the table does not show max concurrency and the model cards only show node counts. Batch route hit rows currently expose `model_key`, `node_id`, `node_name`, and `image_count`; the UI cannot consistently show the model name for hit rows.

## Goals / Non-Goals

**Goals:**
- Default upload routing should prefer a concrete OCR model so a PaddleOCR setup does not silently cross-dispatch to Ollama/DeepSeek.
- Users should be able to see model-level and node-level OCR concurrency capacity.
- Batch route hit rows should distinguish OCR model from node and image count.
- Explicit global load balancing remains available when the user chooses it.

**Non-Goals:**
- Do not remove the Ollama model definition or legacy normalization.
- Do not remove document-level model affinity.
- Do not change OCR request execution, retry, or failover mechanics.
- Do not introduce a new database schema.

## Decisions

- The upload selector will initialize to `MODEL_LOAD_BALANCE` for a preferred available model, choosing PaddleOCR when present and falling back to the first available model. If no models have loaded yet, the form keeps the existing valid empty state until model data arrives.
- The OCR model API response will include aggregate capacity fields derived from existing node rows: total max concurrency, active inflight images, enabled capacity, and global-participating capacity.
- The OCR resource UI will show model-level `解析中 / 并发容量` and node-level `最大并发`, plus global eligibility context where useful.
- Batch route hit rows will include model display names by resolving `model_key` against the registry on the backend. The UI will render OCR model and provider/node details as separate text so `deepseek-ocr:latest` is not mistaken for the OCR model selection.

## Data Flow

1. Upload page loads OCR models.
2. The routing selector chooses `paddle_ocr` as a model-bound default when available.
3. Upload requests persist a `MODEL_LOAD_BALANCE` policy unless the user explicitly changes to global or node-specific routing.
4. OCR dispatch still uses the same coordinator and node capacity controls.
5. OCR resources and batch detail APIs expose enough metadata for the UI to show capacity and final model attribution.

## Risks / Trade-offs

- Changing the upload default makes cross-model dispatch opt-in rather than default. This is intentional because user expectations on the page are model-centric.
- Users with only Ollama configured still get a working default because the selector falls back to the first available model.
- Model card aggregate capacity may count disabled or non-global nodes differently depending on the label. The UI must label total, enabled, and global capacity explicitly enough to avoid confusion.

## Testing

- Add frontend RED tests for upload routing defaulting to PaddleOCR model load balance after models load.
- Add backend RED tests for model-list aggregate capacity fields and batch hit rows including model names.
- Add frontend RED tests for model/node concurrency display and batch route model clarity.
- Run focused tests for touched frontend/backend units, then broader dashboard build and relevant backend test slices.
