## Why

The document result drawer currently renders Markdown tables as plain paragraph
text, so users cannot scan OCR/LLM outputs that include structured rows.

The upload page also allows users to select an unlimited number of files and
send an oversized multipart request. When the request exceeds the server limit,
the browser only receives `413 Payload Too Large` after attempting the upload,
which is late and unclear.

## What Changes

Add safe table support to the existing Markdown preview path and add upload
preflight limits before the multipart request is sent:

- Render pipe-table Markdown as an HTML table in the result drawer.
- Style rendered tables so wide content remains readable inside the drawer.
- Limit one dashboard upload batch to 30 files and 500 MB total.
- Show a user-facing warning before upload when the selected batch exceeds those
  limits.
- Keep a backend 413 fallback that returns a structured `detail` response for
  clients that bypass the dashboard guard.

## Impact

This affects the dashboard result drawer, dashboard upload form validation, and
server-level upload error handling. It does not change OCR processing, storage,
batch creation semantics, or accepted file types.
