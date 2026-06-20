## Why

After a service restart, operators expect OCR throughput to resume according to
the available OCR cluster capacity. In a deployment with three OCR nodes and ten
slots per node, the expected local dispatch capacity is thirty concurrent page
OCR requests.

The current runtime derives default local OCR concurrency from the maximum
single enabled node capacity, and the server configuration pins the page-task
worker to ten. This can make a thirty-slot OCR cluster behave like a ten-slot
local worker. Combined with FIFO page-task selection, one large PDF can dominate
the visible recovery progress while other documents stay in `OCR_QUEUED`.

## What Changes

- Derive default page-task worker and OCR request executor concurrency from the
  sum of enabled, globally participating OCR node capacities.
- Update the server default page-task worker configuration so it no longer pins
  the worker below aggregate node capacity.
- Add fair queued page-task selection so each scan distributes claimed work
  across documents before returning extra pages from the same large document.
- Keep explicit operator overrides respected when a deployment deliberately sets
  page-task worker or OCR request thread-pool sizes.

## Impact

- Startup recovery can use all configured OCR node slots when the operator has
  not overridden local worker capacity.
- Large PDFs remain resumable but no longer monopolize all newly claimed page
  tasks when other documents have queued pages.
- No database migration is required.
