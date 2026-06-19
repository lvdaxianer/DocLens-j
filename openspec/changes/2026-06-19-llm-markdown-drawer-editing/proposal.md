## Why

The current LLM Markdown configuration panel renders the editable form inside the
same split layout as the configuration table. On narrow or dense screens, the form
visually collides with the table and makes the page feel cramped.

## What Changes

Move the LLM Markdown create/edit experience into a right-side drawer, matching
the interaction pattern already used by OCR node create/edit flows.

## Impact

This changes only the dashboard configuration editing surface. The table, row
actions, save/test behavior, and backend API remain unchanged.
