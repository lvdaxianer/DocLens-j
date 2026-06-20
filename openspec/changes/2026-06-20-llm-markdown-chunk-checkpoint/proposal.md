## Why

Large documents can be split into many Markdown chunks before LLM formatting.
Today those chunk results only live in memory until the whole document is joined
and saved. If the service stops after most chunks have already completed, startup
recovery retries the document but must call the LLM again for every chunk.

## What Changes

Persist successful LLM Markdown chunk outputs as per-document checkpoint files
under the configured local storage root. On a document retry or startup recovery,
the chunked Markdown processor will reuse valid checkpointed chunks and only call
the LLM for missing chunks.

## Impact

This changes the Markdown post-processing internals and adds a recoverable
checkpoint directory for chunked documents. The public upload APIs, OCR result
contract, final Markdown result path, and dashboard rendering contract remain
unchanged.
