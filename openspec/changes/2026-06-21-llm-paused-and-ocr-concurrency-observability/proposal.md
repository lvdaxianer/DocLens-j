## Why

When all LLM Markdown configs are paused, DocLens currently presents some results
as `LLM 排版失败` if a stale enabled config still reaches credential resolution.
That message makes an intentional pause look like a broken LLM call.

The OCR concurrency dashboard also hides the configured executor capacity. Users
can see active requests and queue depth, but not whether the page-task worker and
OCR request executor actually booted with the sum of page-configured OCR node
concurrency.

## What Changes

- Add an explicit paused / unavailable LLM result state so intentionally paused
  configs return OCR text without displaying an LLM failure.
- Keep real attempted LLM failures visible, including credential and transport
  errors, only when a config was actually eligible and selected.
- Expose configured OCR executor and page-task worker capacities in dashboard
  metrics.
- Add a regression test proving multiple queued pages for the same document can
  be submitted concurrently to one OCR node when that node has available slots.

## Impact

The change only affects result-state wording and observability. It does not change
OCR routing policy, LLM prompts, provider request payloads, or the rule that an
enabled complete LLM config is attempted even when the latest health check is
unhealthy.
