## 1. Chunk-parallel Markdown execution

- [x] 1.1 Add a failing test that proves a large Markdown document submits all
  chunks into a shared worker pool, allows chunk work to overlap, and merges the
  successful Markdown chunks back in chunk order.
- [x] 1.2 Add a failing test that proves two documents can share the same chunk
  executor at the same time, so the second document does not wait for the first
  document's chunk sequence to finish before starting its own chunk work.
- [x] 1.3 Implement a chunk execution coordinator that submits each chunk as an
  independent task, keeps the current round-robin config selection and per-config
  rate limiting, and joins ordered chunk results back into the final document.
- [x] 1.4 Extend the chunk execution tests so a chunk that exhausts retries falls
  back to its original chunk text while the remaining chunks still complete and
  merge normally.
- [x] 1.5 Re-run the focused backend tests for Markdown chunking and the broader
  processing tests that cover batch/document completion.
