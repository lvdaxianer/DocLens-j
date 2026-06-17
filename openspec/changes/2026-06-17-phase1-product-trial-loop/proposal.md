## Why

DocLens-j already lets users upload files, view batch status, and inspect OCR
results in the Dashboard. The first product roadmap stage asks for a clearer
trial loop: a new user should be able to start the service, upload a document,
open the result, and take the result into another workflow within minutes.

The current Dashboard result drawer supports preview and copy, but it does not
offer direct result downloads. The documentation also lacks a dedicated
five-minute path that guides a user from local startup to result consumption.

## What Changes

Implement a focused Stage 1 slice:

- Add direct Markdown, TXT, and JSON downloads from the document result drawer.
- Add a five-minute trial guide that walks through local startup, upload,
  batch inspection, and result download.
- Add a visible Dashboard upload-page entry to the trial guide so first-time
  users can find it while trying the product.

## Impact

This change affects the Dashboard result drawer and documentation. It does not
change backend OCR processing, storage, database schema, authentication, or
callback delivery behavior.
