## 1. Checkpoint file store

- [x] 1.1 Add failing tests and implement a filesystem chunk checkpoint store
  that writes `manifest.json`, `<chunkNo>/README.md`, and `<chunkNo>/meta.json`
  under `llm-markdown-chunks/<documentId>/`, using zero-padded chunk
  directories, checksum metadata, and temp-file atomic moves.
- [x] 1.2 Add focused tests that reject stale checkpoints when the current plan
  identity differs from the persisted manifest.

## 2. Chunk processor recovery

- [x] 2.1 Add a failing processor test proving valid checkpointed chunks are
  returned without calling the LLM delegate, while missing chunks still call the
  delegate and are merged in order.
- [x] 2.2 Wire checkpoint lookup and successful chunk checkpoint scheduling into
  the chunked Markdown processor without changing the public
  `MarkdownPostProcessor` contract.
- [x] 2.3 Add a focused test proving fallback chunks produced after exhausted
  retries are not checkpointed and will be retried in a later run.

## 3. Crash-resume and async durability

- [x] 3.1 Add a crash-resume simulation test that seeds completed chunk files for
  an interrupted document and proves the next run processes only missing chunks.
- [x] 3.2 Add an async-save verification test proving checkpoint writes are not
  performed inline in the LLM worker path, but the processor waits for pending
  checkpoint writes before returning the final document result.
- [x] 3.3 Implement checkpoint write coordination and error handling so write
  failures are logged without replacing successful in-memory chunk results.

## 4. Spring wiring and cleanup

- [x] 4.1 Add auto-configuration tests for the default filesystem checkpoint
  store and its checkpoint write executor under the configured storage root.
- [x] 4.2 Wire the checkpoint store and async write executor into
  `ConfigurableMarkdownPostProcessor` and `ChunkedMarkdownPostProcessor`.
- [x] 4.3 Extend document and batch delete behavior so checkpoint directories are
  removed when documents or batches are explicitly deleted.

## 5. Verification and archive

- [x] 5.1 Re-run focused Markdown chunking, checkpoint store, delete, and startup
  recovery tests plus the relevant broader backend test slice.
- [x] 5.2 Run strict OpenSpec validation, plan-implementation audit, and
  code-review-spec over the completed diff.
- [x] 5.3 Archive the completed OpenSpec change after all tasks pass.
