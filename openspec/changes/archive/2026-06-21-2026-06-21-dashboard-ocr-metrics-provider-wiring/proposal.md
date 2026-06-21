## Why

The OCR node management API shows persisted OCR nodes and recent successful node
calls, but the Dashboard batch detail and OCR health endpoints return empty OCR
resources and empty allocation rows. This makes the UI show blank "running
allocation" and "final allocation" sections even after OCR pages were processed.

## What Changes

Ensure Dashboard queries are wired to the real `OcrDashboardMetricsProvider`
when OCR node repositories, call repositories, runtime node pool, and model
registry are available. The empty Dashboard metrics provider must remain only as
a fallback for embedded hosts that do not enable OCR resource routing.

## Impact

Dashboard batch detail and OCR health responses will expose the same OCR node
capacity and call attribution data already visible through OCR node management
APIs. This change does not alter OCR routing, OCR execution, node health checks,
or page-task scheduling.
