## Why

The technical delivery document currently uses a sequence diagram for the
end-to-end processing path. That diagram shows component order, but it does not
make the concurrency queues, worker replenishment, page-task loop, OCR slot
waiting, retry path, and final batch completion easy to evaluate.

## What Changes

Replace the delivery document's end-to-end sequence diagram with a flowchart
based on a concrete scenario: 100 documents uploaded at the same time and
processed until every document reaches a terminal parsed or failed state. Update
the concurrency example text to explain how six document-processing workers
start immediately, how the remaining documents wait in the executor queue, and
how new documents are pulled as workers finish preparation.

## Impact

This is documentation-only. It does not change runtime behavior, configuration
defaults, database schema, APIs, worker scheduling, or OCR dispatch logic.
