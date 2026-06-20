## Context

`DocumentResultDrawer` already uses `renderMarkdownPreviewHtml` to render a
small safe Markdown subset. The renderer escapes user content before producing
HTML and currently handles headings, paragraphs, bold text, and safe links.
Tables are not recognized, so pipe-table Markdown is folded into a paragraph and
displayed literally.

`UploadDropzone` owns the selected `File[]` state and emits the final
`UploadBatchOptions` to `UploadView`. It already performs submit-time validation
for metadata JSON and OCR routing. This is the right boundary for a batch-level
file count and total byte preflight because it can block the submit event before
the store calls `uploadBatch`.

The server currently sets Spring multipart limits to 300 MB. The product limit
for the dashboard should become 500 MB total, so the server limit must be aligned
and must return structured upload errors when a request still exceeds the cap.

## Goals / Non-Goals

**Goals:**
- Render Markdown pipe tables safely in the result drawer.
- Keep existing Markdown escaping and unsafe-link blocking.
- Show table content in a scrollable, compact table layout.
- Block dashboard upload submission when more than 30 files are selected.
- Block dashboard upload submission when selected files exceed 500 MB total.
- Align server multipart request size with the 500 MB dashboard limit.
- Return a structured `detail` message for oversized multipart requests.

**Non-Goals:**
- Do not introduce a full Markdown dependency for this change.
- Do not change the upload API request shape.
- Do not change accepted file types.
- Do not add per-file size limits unless a later product decision requires it.
- Do not change OCR worker concurrency or processing retry behavior.

## Decisions

- Extend the existing safe renderer instead of adding a Markdown package. This
  keeps the implementation narrow and preserves the current HTML escaping model.
- Recognize a Markdown table only when a block has a header row, a separator row,
  and at least one body row. Invalid table-like content remains a paragraph.
- Support common alignment markers in the separator row for valid table syntax,
  but do not expose alignment UI yet; styling stays consistent and compact.
- Store upload thresholds in dashboard constants/utilities so UI validation and
  tests share one source of truth.
- Use 30 files and 500 MB as the dashboard batch preflight thresholds approved by
  the user.
- Raise Spring multipart `max-file-size` and `max-request-size` to 500 MB to keep
  server behavior aligned with the dashboard total upload cap.

## Component Map

- `DocumentResultDrawer.vue`: consumes rendered Markdown HTML and owns drawer
  table styling only.
- `markdownPreviewRules.ts`: pure utility that parses and escapes Markdown
  preview HTML, including table rendering.
- `UploadDropzone.vue`: orchestration component that validates selected file
  count and total byte limits before emitting `submit`.
- `uploadBatchRules.ts` or existing upload rules utility: pure upload preflight
  constants and validation helpers.
- `GlobalExceptionHandler.java`: maps multipart size exceptions to a structured
  HTTP 413 response.

## Data Flow

1. A completed document result is loaded into `DocumentResultDrawer`.
2. The drawer derives `markdownPreviewHtml` from `result.finalText`.
3. `renderMarkdownPreviewHtml` splits Markdown blocks and renders valid table
   blocks as escaped table HTML.
4. The drawer displays the rendered table inside the existing result preview
   container.

Upload flow:

1. User selects or drops files into `UploadDropzone`.
2. `UploadDropzone` stores the selected files and shows the file list summary.
3. User clicks `上传并解析`.
4. `UploadDropzone` validates OCR routing, metadata JSON, file count, and total
   size.
5. If limits are exceeded, it shows a warning and does not emit `submit`.
6. If limits pass, it emits the existing `UploadBatchOptions` unchanged.

## Error Handling

- More than 30 selected files: warn the user that one batch can upload at most 30
  files.
- More than 500 MB selected total: warn the user that one batch can upload at
  most 500 MB and ask them to split the batch.
- Oversized multipart request reaching the server: return HTTP 413 with a JSON
  body containing a `detail` field, so dashboard fallback handling can show a
  friendly message.

## Risks / Trade-offs

- A lightweight table parser will not cover every Markdown edge case. That is
  acceptable for OCR/LLM pipe tables and avoids widening dependencies.
- Keeping `v-html` means the renderer must continue escaping all user content
  before producing HTML. Tests must cover script escaping and unsafe links after
  table support is added.
- A 500 MB limit permits larger uploads than the current server config, which may
  consume more memory and network time. The dashboard preflight reduces failed
  attempts, and processing concurrency remains unchanged.

## Testing

- Add a failing utility test for Markdown table rendering, including escaped cell
  content.
- Keep existing Markdown tests for headings, paragraphs, script escaping, and
  unsafe link blocking green.
- Add a failing upload validation test that selecting 31 files blocks `submit`
  and shows a warning.
- Add a failing upload validation test that selecting files over 500 MB blocks
  `submit` and shows a warning.
- Add or update an API/server test proving multipart limit config is 500 MB.
- Add a server exception handler test proving oversized upload errors return
  HTTP 413 with structured `detail`.
