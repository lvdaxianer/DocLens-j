## Context

`LlmMarkdownConfigPanel` currently renders a two-column body: the table on the
left and the form editor on the right. That layout works for wide screens but
competes with the table for horizontal space and causes the overlap issue noted
in the screenshot.

The OCR node flows in the same dashboard already use a right-side drawer for
create/edit, so the safest and most consistent fix is to reuse that interaction
shape for LLM Markdown configuration editing as well.

## Goals / Non-Goals

**Goals:**
- Move LLM Markdown create/edit into a right-side drawer.
- Keep the configuration list visible and stable while editing.
- Reuse the existing form state, validation, save, and test behavior.
- Keep the existing row actions for edit, enable/disable, default, and delete.

**Non-Goals:**
- Do not change the backend LLM Markdown configuration API.
- Do not redesign the table columns or row actions.
- Do not change the existing validation rules or payload shape.

## Decisions

- Use one drawer for both create and edit.
- Open the drawer from the existing `新增配置` button and from the table row
  `编辑` action.
- Keep the form fields and action buttons inside the drawer footer, following the
  same mental model as the OCR node form drawer.
- Preserve the existing `testConfig` and `saveConfig` flows, only relocating the UI.

## Data Flow

1. User clicks `新增配置` or `编辑` on a row.
2. The panel switches the shared LLM form state to create or edit mode.
3. A right-side drawer opens with the current form values.
4. User edits fields, tests the configuration, and saves from the drawer.
5. On save success, the row list updates in place and the drawer remains in sync
   with the saved configuration.

## Risks / Trade-offs

- The drawer is a larger surface than the current inline form, so some controls
  may sit below the fold on smaller screens. This is acceptable because the list
  remains readable and the drawer can scroll internally.
- Moving the form into a drawer slightly increases component boundaries, but it
  eliminates the current table/editor overlap and aligns the UX with the rest of
  the dashboard.

## Testing

- Add a failing component test that proves `新增配置` opens a drawer instead of
  rendering the editor inline.
- Add a test that the existing edit action also opens the same drawer with row
  data.
- Re-run the UI test slice that covers the LLM configuration panel and any
  affected utility tests.
