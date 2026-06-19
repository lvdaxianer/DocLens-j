## 1. LLM configuration drawer editing

- [x] 1.1 Add a failing UI test that proves the LLM Markdown panel opens a right-side drawer when the user clicks `新增配置`, and that the inline editor is no longer rendered beside the table.
- [x] 1.2 Update the LLM Markdown config panel and composable so create/edit state is driven by a shared drawer, while preserving the existing save/test/delete/default row behavior.
- [x] 1.3 Add or adjust a UI test for the edit action so clicking a table row `编辑` button opens the same drawer with row data.
- [x] 1.4 Re-run the focused UI tests and the dashboard build to confirm the drawer layout and interactions are stable.
