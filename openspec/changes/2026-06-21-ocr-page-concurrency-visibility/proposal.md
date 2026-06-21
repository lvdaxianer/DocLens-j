## Why

Operators can currently see OCR routing at node-aggregate level: a document has
N running images on a node, and a batch has N hits on a node. That does not prove
which page image is running, which local worker thread is consuming it, or
whether two pages are actually in flight at the same time.

When a two-page document appears slow, the Dashboard needs enough live detail to
differentiate these cases:

- both pages are running concurrently on different worker threads;
- both pages are assigned to one OCR node but are still concurrent;
- only one page is running and the second page is queued;
- OCR has completed and the document is waiting in a later stage such as LLM.

## What Changes

- Track live page-level OCR execution details while a page task is being
  processed.
- Include page number, document ID, OCR node, model, worker ID, thread name,
  start time, and running duration in the batch detail read model.
- Show a "实时 OCR 图片任务" table in the batch detail OCR route panel.
- Keep existing aggregate running/final allocation cards for quick summaries.
- Remove live page detail as soon as the page finishes or fails so the panel only
  represents current in-flight OCR work.

## Impact

- Operators can directly verify concurrent image consumption by comparing
  simultaneous rows and thread names.
- Existing persisted OCR call history remains unchanged.
- The new live detail is best-effort runtime state; after process restart it may
  be empty until new page tasks start.
- Completed pages remain visible through the existing final allocation summary
  and persisted OCR node call history, not through the live table.
