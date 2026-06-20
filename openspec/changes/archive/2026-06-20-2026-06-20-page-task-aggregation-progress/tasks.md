## 1. Advance page-task aggregation progress before slow LLM

- [x] 1.1 Add a failing regression test proving the final page aggregation advances the document to `SAVE_TEXT` before Markdown post-processing completes.
- [x] 1.2 Update page-task aggregation to persist `MERGE_TEXT` and `SAVE_TEXT` progress in short transactions before the slow LLM call.
- [x] 1.3 Run focused and broader backend verification, then audit the implementation against this OpenSpec change.
- [x] 1.4 Archive this completed OpenSpec change after implementation and final audit.
