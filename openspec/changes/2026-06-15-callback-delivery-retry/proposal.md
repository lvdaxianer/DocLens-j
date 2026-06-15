## Why

DocLens currently records callback contract data in completion events, but it
does not actually deliver the external `callback_url` after OCR success.

## What Changes

Add a durable callback delivery flow that persists callback jobs, dispatches
HTTP POST requests asynchronously, records success or failure, captures a
machine-readable failure reason plus human-readable failure detail, and retries
failed deliveries with bounded backoff.

## Impact

The change adds a new callback worker path, callback job persistence, and
callback outcome visibility without changing the existing OCR extraction flow.
