## Why

The root README files and technical delivery document need to be realigned with
the current implementation after the recent OCR scheduling, crash recovery,
LLM Markdown checkpointing, upload guarding, environment credential, and
dashboard observability changes.

The current documents describe the project at a high level, but they do not yet
give readers a complete, current entry point for:

- page-level OCR concurrency and global load balancing;
- restart recovery for queued batches and page tasks;
- persisted LLM Markdown chunk checkpoints;
- upload limits and Markdown table rendering behavior;
- dashboard visibility for OCR runtime allocation;
- the supported deployment and integration shapes.

## What Changes

Rewrite the Chinese README, English README, and technical delivery document as a
focused documentation refresh:

- `README.md` becomes the Chinese project entry point.
- `README-en.md` becomes the English project entry point.
- `docs/technical-delivery.md` becomes the deeper architecture and operations
  reference.
- The stale uppercase `README_EN.md` entry is removed so the repository has one
  canonical English README path.

## Impact

This is documentation-only. It does not change runtime behavior, database
schema, public API contracts, frontend behavior, or build configuration.
