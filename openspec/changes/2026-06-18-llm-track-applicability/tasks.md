## 1. Dashboard Track Semantics

- [x] 1.1 Add a focused regression test in the batch-detail query suite that proves a completed Markdown document shows `LLM 排版` as applicable instead of skipped.
- [x] 1.2 Update `ProcessingTrackAssembler` so the `LLM 排版` node no longer depends on `ProcessingTrackProfile.hasMerge()` and instead uses a dedicated applicability rule for the LLM post-processing stage.
- [x] 1.3 Run the focused query tests and the relevant broader test target, then validate the OpenSpec change and review the diff.
