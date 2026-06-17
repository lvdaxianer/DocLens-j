## Why

DocLens-j already has OCR batch processing, SDK, HTTP service, Dashboard,
node governance, callback delivery, LLM Markdown post-processing, and
OpenWebUI integration. The project now needs a durable product roadmap that
explains how these technical capabilities become a usable document parsing
infrastructure product.

The roadmap must reflect the product decision that DocLens-j is part of a
larger workflow. It should identify calling systems through request
credentials, not introduce a personal login or user account system.

## What Changes

Add a product roadmap document under `docs/` that defines:

- Stage 1: product trial and result-consumption loop.
- Stage 2: caller credentials and request governance.
- Stage 3: scenario templates and differentiation.
- MVP boundaries, success metrics, risks, and recommended execution order.

## Impact

This is documentation-only product planning. It does not change runtime code,
API behavior, configuration, database schema, or Dashboard implementation.
