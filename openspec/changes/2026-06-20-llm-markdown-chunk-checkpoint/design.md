## Context

DocLens already has document-level startup recovery and document retry. It also
processes large Markdown inputs by creating an ordered `MarkdownChunkPlan` and
submitting chunk work into a shared LLM Markdown chunk executor. The missing
piece is a durable boundary inside the document: after a chunk succeeds, the
result is not persisted until every chunk has completed and the final Markdown
document is saved.

The desired behavior is chunk-level crash resume. If a document has 100 chunks
and the service stops after 90 chunks are formatted and checkpointed, the next
document retry should load those 90 chunk outputs from disk and process only the
remaining 10 chunks.

## Goals / Non-Goals

**Goals:**
- Persist successful LLM-formatted chunk outputs to disk using deterministic
  per-document, per-chunk paths.
- Reuse valid checkpointed chunks after service restart, document retry, or
  startup recovery.
- Avoid reusing stale checkpoints when OCR text, chunk strategy, chunk count, or
  max context settings change.
- Preserve ordered final Markdown joins.
- Keep checkpoint writes off the hot LLM request path as much as possible while
  avoiding a successful document result before the current run's checkpoint
  writes have completed.
- Avoid caching chunk fallback text produced after exhausted retries.
- Clean checkpoint files when documents or batches are explicitly deleted.

**Non-Goals:**
- Do not add a public API for browsing or editing checkpoint chunks.
- Do not change the final OCR result schema or final Markdown storage URI.
- Do not change chunk sizing, overlap, prompt content, or LLM retry counts.
- Do not introduce a database table for chunk checkpoints in this change.

## Decisions

- Add a small checkpoint port that the chunked Markdown processor can call to
  load existing chunk results and save new successful chunk results.
- Implement the default checkpoint store with the local filesystem rooted at
  `doclens.storage-root`.
- Store checkpoints under `llm-markdown-chunks/<documentId>/` with:
  - `manifest.json` for document and plan identity.
  - `<chunkNo>/README.md` for the formatted Markdown output.
  - `<chunkNo>/meta.json` for chunk metadata and checksum.
- Use zero-padded chunk directories. Width is at least two digits and grows with
  `chunkCount`, so 50 chunks use `01`, while 100 chunks use `001`.
- Treat a checkpoint as valid only when the manifest matches the current plan
  fingerprint and the chunk has both a non-empty `README.md` and matching
  metadata.
- Write files through temporary paths and atomic moves when the filesystem
  supports them. Partial files must not be accepted as valid checkpoints.
- Schedule checkpoint saves asynchronously after a chunk succeeds, then wait for
  all checkpoint write futures before returning the final document result from
  the chunked processor.
- If a checkpoint write fails, keep the in-memory chunk result so the document
  can continue. The failed checkpoint may cause that chunk to be reprocessed on a
  later crash retry, which is preferable to failing an otherwise good document.
- Retain checkpoint directories after document completion for recovery/debugging
  and delete them when the document or batch is deleted.

## Data Flow

1. A document reaches Markdown post-processing with full OCR text.
2. The chunker builds an ordered plan and a plan fingerprint is derived from the
   document id, file name, chunk strategy, max context tokens, chunk count, and
   chunk content identity.
3. The checkpoint store loads or initializes `manifest.json` for the document.
4. For every chunk:
   - If the manifest and chunk checkpoint are valid, return the checkpointed
     Markdown without calling the LLM.
   - Otherwise call the LLM with the existing chunk prompt and retry behavior.
   - If the chunk returns successful Markdown, schedule an async checkpoint save.
   - If the chunk falls back to original text, return it in memory but do not
     checkpoint it.
5. The coordinator joins chunk results by chunk index, waits for current
   checkpoint save futures to finish, and returns the final Markdown result.
6. The existing result builder saves the joined Markdown result exactly as it
   does today.

## File Layout

Example for document `yyyy` with 50 chunks:

```text
<storage-root>/llm-markdown-chunks/yyyy/manifest.json
<storage-root>/llm-markdown-chunks/yyyy/01/README.md
<storage-root>/llm-markdown-chunks/yyyy/01/meta.json
<storage-root>/llm-markdown-chunks/yyyy/02/README.md
<storage-root>/llm-markdown-chunks/yyyy/02/meta.json
```

Example for 100 chunks:

```text
<storage-root>/llm-markdown-chunks/yyyy/001/README.md
<storage-root>/llm-markdown-chunks/yyyy/100/README.md
```

## Risks / Trade-offs

- Async checkpoint writes mean a crash between LLM success and durable write can
  still reprocess a small number of just-finished chunks. Waiting for all pending
  writes before document completion keeps completed documents durable without
  blocking each LLM request on disk IO.
- Local filesystem checkpoints work for the current local storage model. If the
  service later runs multiple stateless nodes against remote object storage, the
  checkpoint store can be replaced behind the port.
- Retaining checkpoints after completion consumes disk space. Deleting them on
  explicit document or batch deletion prevents unbounded growth for removed
  data, while keeping recovery/debug value for active results.

## Testing

- Add filesystem store tests for deterministic paths, manifest validation,
  non-empty chunk validation, atomic write behavior, and stale checkpoint
  rejection.
- Add processor tests proving existing valid checkpoints skip LLM calls and
  missing chunks still call the delegate.
- Add processor tests proving successful chunks schedule checkpoint saves while
  fallback chunks are not cached.
- Add a crash-resume simulation test: the first processor instance creates some
  checkpoint files, the second instance reuses them and processes only missing
  chunks.
- Add deletion tests proving document and batch delete remove checkpoint
  directories together with stored source/result files.
- Re-run focused backend tests for Markdown chunking, storage, delete behavior,
  and the broader processing test slice.
