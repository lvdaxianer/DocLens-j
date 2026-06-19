## Why

Some upstream systems may already have applied LLM-based orchestration or Markdown normalization before sending documents to DocLens. Today DocLens always runs its own Markdown post-processing path, which can repeat work unnecessarily and may rewrite text that has already been normalized upstream.

## What Changes

- Add an upload-time boolean flag to mark documents as already LLM-orchestrated.
- Propagate that flag through the batch creation request and document job model.
- Skip DocLens Markdown post-processing when the flag is set, while preserving the existing non-orchestrated behavior.
- Keep the current Markdown post-processing path unchanged for uploads that do not set the flag.

## Capabilities

### New Capabilities
- `llm-markdown-orchestration-skip`: allow uploads to declare that upstream LLM orchestration already happened so DocLens does not run Markdown post-processing again.

### Modified Capabilities
- `project-documentation`: update the upload and document-processing behavior documentation to include the new upload flag and skip semantics.

## Impact

This affects the upload request contract, batch creation mapping, document job persistence, and the Markdown post-processing entry point in the backend. The dashboard upload form will also need a new field so users can opt into the skip behavior when appropriate.
