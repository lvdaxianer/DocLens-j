## Why

The new x86 file-mounted runtime delivery layout is visible and runnable, but a
real smoke test exposed two gaps:

1. the mounted `runtime.env` does not fully control the backend process, so the
   Java runtime still starts with the packaged Spring profile and config search
   path instead of the mounted values
2. the packaged frontend assets are present, but the documented `/dashboard/`
   entry returns `404` unless the operator opens `/dashboard/index.html`

Both behaviors weaken the operator promise of file-mounted delivery because the
mounted runtime file should drive startup, and the documented dashboard path
should be directly reachable.

## What Changes

- make the packaged backend startup script re-load the mounted runtime env file
  before launching Java so mounted startup values reliably reach the backend
  process
- add a server-side dashboard entry mapping so `/dashboard` and `/dashboard/`
  resolve to the bundled dashboard index page
- extend focused delivery verification so the mounted runtime hook and
  dashboard entry are both covered

## Impact

This is a localized packaging/runtime bug fix. It does not change database
schema, OCR behavior, or Helm topology.
