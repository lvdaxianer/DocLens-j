## Context

The current Markdown post-processing pipeline does three things in sequence:
it selects one healthy LLM configuration by round-robin, applies per-config rate
limits, and then processes large Markdown inputs by iterating over chunks one at
a time. That means the system can use multiple LLM configurations over time, but
it cannot consume chunk workloads concurrently from a single document or across
multiple documents.

The desired behavior is to treat every Markdown chunk as an independent work
item. All chunks should be submitted into a shared worker pool, each chunk should
pick a healthy LLM configuration at execution time, and the chunk results should
be joined back into the final document in original chunk order.

## Goals / Non-Goals

**Goals:**
- Execute Markdown chunks concurrently within one document and across multiple
  documents.
- Keep configuration selection round-robin across healthy eligible configs.
- Keep per-config concurrency and request-interval enforcement.
- Preserve retry behavior for each chunk.
- When a chunk exhausts retries, fall back to that chunk's original text and
  continue merging the rest of the document.
- Preserve the existing document- and batch-level save/event behavior.

**Non-Goals:**
- Do not change OCR extraction or page task scheduling.
- Do not change the saved OCR result schema.
- Do not add a new public API for manual chunk submission.

## Decisions

- Use a shared executor for Markdown chunk tasks so all chunk workloads compete
  for the same worker capacity.
- Keep `LlmConfigSelector` as the configuration chooser, but call it from chunk
  execution rather than once per document.
- Keep `LlmConfigRateLimiter` as the per-config guard for concurrency and
  request spacing.
- Add a chunk execution coordinator that joins chunk futures, preserves order,
  and merges successful Markdown outputs with fallback original text for failed
  chunks.
- Keep the existing `MarkdownPostProcessor` contract synchronous for callers by
  making the parallelism an internal implementation detail.

## Data Flow

1. A document reaches Markdown post-processing with full OCR text.
2. The chunker produces an ordered chunk plan.
3. Every chunk is submitted to a shared executor as an independent task.
4. Each chunk task selects a healthy LLM config, acquires the config permit,
   retries on failure, and eventually either returns Markdown or its original
   chunk text.
5. The coordinator waits for all chunk futures, sorts by chunk index, and joins
   the final Markdown text.
6. The joined result is returned to the existing document result builder and is
   saved exactly as before.

## Risks / Trade-offs

- Parallel chunk execution increases throughput but also increases peak pressure
  on the LLM providers and the shared executor.
- Because chunk order must be preserved, the coordinator needs to buffer all
  chunk results before it can build the final document text.
- Chunk-level fallback to original text can hide partial quality regressions if
  too many chunks fail, so warnings and metadata need to remain visible.

## Testing

- Add a failing unit test that proves chunk tasks from one document are executed
  in parallel against multiple LLM configs and then merged in order.
- Add a failing unit test that proves multiple documents can feed the same chunk
  worker pool concurrently.
- Add a failing unit test that proves a failed chunk falls back to the original
  chunk text while successful chunks still keep their Markdown output.
- Re-run the focused backend tests and the broader processing test slice.
