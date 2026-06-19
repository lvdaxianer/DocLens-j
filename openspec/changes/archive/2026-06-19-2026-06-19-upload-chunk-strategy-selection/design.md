## Context

DocLens currently uses a single Markdown chunking policy for all uploaded
content. The core chunker already splits long text using a sliding-window plan,
but the window size is effectively global. That makes the default behavior easy
to understand, yet it also means the same chunk shape is used for news, technical
articles, and academic documents even though those document types benefit from
very different context budgets.

## Goals / Non-Goals

**Goals:**
- Let the uploader choose a document-type preset during file upload.
- Keep the default experience simple by selecting a general preset initially.
- Map the preset to concrete chunking defaults on the server before chunking.
- Preserve existing uploads that do not send the new field by falling back to the
  general preset.

**Non-Goals:**
- Do not expose free-form numeric chunk size editing in the first release.
- Do not add post-upload re-chunking controls in document detail views.
- Do not change the core sliding-window algorithm beyond preset-aware defaults.

## Decisions

- The dashboard will render a dedicated chunk strategy selector alongside the
  existing advanced upload fields.
- The multipart upload payload will include a new preset field that uses a stable
  enum-like value, not a localized label.
- The backend will translate the preset into chunking defaults close to the
  chunker boundary so the UI stays descriptive while the core logic stays simple.
- The general preset will remain the default to keep the current behavior for
  users who do not think about chunking at all.
- The first release uses fixed defaults of `GENERAL` = `400/80`,
  `NEWS` = `300/60`, `TECHNICAL` = `500/100`, and `ACADEMIC` = `800/150`
  for `chunk_size/overlap`.

## Data Flow

1. The user opens the upload page and selects files.
2. The user picks a chunking preset such as news, technical, or academic.
3. The dashboard sends the preset with the multipart upload request.
4. The request mapper stores the preset on the batch command.
5. The backend resolves the preset into chunk window defaults.
6. The Markdown chunker uses those defaults when it creates the chunk plan.

The fixed mappings for the first release are `GENERAL` = `400/80`,
`NEWS` = `300/60`, `TECHNICAL` = `500/100`, and `ACADEMIC` = `800/150` for
`chunk_size/overlap`.

## Risks / Trade-offs

- Preset names must stay consistent across the dashboard, API, and backend, so we
  need to keep the enum and request field names centralized.
- A preset-based approach is less flexible than raw numeric editing, but it is
  much safer for first-time users and easier to support.
- Changing the default window sizes later will affect chunk shape, so the
  defaults should be documented and tested.

## Testing

- Add a UI test that the selector shows the available presets and defaults to the
  general preset.
- Add an API test that the multipart payload includes the selected preset field.
- Add a backend test that preset resolution maps news, technical, and academic
  presets to different chunking defaults.
- Run the dashboard build and the focused backend tests to confirm the new field
  flows through the stack.
