## Why

Users currently upload documents without any way to choose a chunking preset,
so the backend always applies the same sliding-window defaults. That works for
mixed content, but it is a poor fit for short news posts, dense technical
articles, and long academic writing.

## What Changes

Add a chunking preset selector to the upload flow and carry the selected preset
through the upload request so the backend can resolve preset-specific sliding-
window defaults before chunking begins.

The initial presets are fixed as:

- `GENERAL` -> `400/80`
- `NEWS` -> `300/60`
- `TECHNICAL` -> `500/100`
- `ACADEMIC` -> `800/150`

## Impact

This changes the upload form, the multipart request payload, and the server-side
chunking configuration path. The first release uses fixed presets only; it does
not expose raw numeric window editing in the UI.
