## Overview

This change corrects the Dashboard processing-track semantics for the `LLM 排版` step.
The current implementation uses file-type merge capability as a proxy for LLM applicability,
which incorrectly marks Markdown/TXT documents as skipped even though they still flow
through the Markdown post-processing service.

## Approach

Keep the existing eight-step track shape. Preserve file-type-based skip behavior for
conversion, page rendering, OCR, and merge steps. Make `LLM 排版` independent from
`hasMerge` and treat it as applicable for all document types currently supported by the
pipeline.

This keeps the fix local to the Dashboard read model and avoids coupling query code to
runtime LLM configuration state.

## Validation

Add a regression test in the batch-detail query suite that covers a completed Markdown
document and asserts `LLM 排版` is shown as an active/completed step rather than skipped.
The existing Word/PDF assertions should continue to prove that earlier steps are still
skipped where appropriate.
