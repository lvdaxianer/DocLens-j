## Context

DocLens already records chunk metadata in the OCR result payload and already runs
chunked Markdown work through a dedicated executor. However, the dashboard batch
detail page reads document rows from the query layer, and those rows currently do
not carry any chunk-specific fields. The OCR resource cards also show only OCR
request and health queues, so they do not explain whether Markdown chunking is
busy.

## Goals / Non-Goals

**Goals:**
- Surface the planned Markdown chunk count in batch detail document rows.
- Surface the LLM Markdown chunk executor activity in the OCR resource panel.
- Keep the chunking algorithm and preset sizing unchanged.
- Keep the change read-only from a product behavior perspective.

**Non-Goals:**
- Do not change chunk size, overlap, or scheduling behavior.
- Do not add a chunk progress API or per-chunk progress bars.
- Do not add a new user-editable performance setting.

## Decisions

- Reuse the existing `llm_chunk_count` metadata already emitted by the chunked
  Markdown path and project it into dashboard document rows.
- Add a dedicated LLM Markdown chunk executor metric card rather than overloading
  the generic OCR request queue card.
- Keep the existing `LLM 排版中` stage label, but augment it with chunk count so
  users can see the scale of the work in progress.

## Data Flow

1. Markdown post-processing emits chunk metadata with `llm_chunk_count`.
2. The dashboard query layer reads the saved OCR result and projects chunk count
   into each document row for batch detail.
3. The dashboard OCR metrics provider reads the dedicated chunk executor metrics.
4. The dashboard UI renders the chunk count next to the document stage and shows
   the chunk executor activity in the resource cards.

## Risks / Trade-offs

- The chunk count is an estimate of work planning, not a live per-chunk progress
  indicator, so the UI must not imply exact completion percentage.
- Exposing executor queue size may still not reveal the true bottleneck if the
  provider LLM itself is slow, but it gives users a much clearer signal than the
  current single-stage label.

## Testing

- Add a backend test proving batch detail document rows include the chunk count.
- Add a backend test proving the OCR resource metrics include the chunk executor.
- Add a dashboard test proving the batch detail page renders chunk count text.
- Add a dashboard test proving the OCR resource cards include the chunk executor
  queue / active state.
