## 1. Live OCR page task tracking

- [ ] 1.1 Add failing backend tests for recording, snapshotting, and removing live OCR page task rows.
- [ ] 1.2 Implement a runtime live page task tracker with page number, worker ID, thread name, start time, and selected node/model fields.
- [ ] 1.3 Wire the page task executor to record live rows around OCR routing and clean them up on success or failure.
- [ ] 1.4 Run focused backend live tracker and page task execution tests.

## 2. Batch detail read model

- [ ] 2.1 Add failing backend tests proving batch detail exposes `ocr_running_page_tasks`.
- [ ] 2.2 Add the live page task rows to Dashboard OCR metrics and batch detail assembly.
- [ ] 2.3 Run focused Dashboard query tests.

## 3. Dashboard display

- [ ] 3.1 Add failing frontend tests for rendering live OCR page task rows and thread names.
- [ ] 3.2 Render a "实时 OCR 图片任务" table in the batch OCR route panel.
- [ ] 3.3 Run focused Dashboard component tests.

## 4. Verification and archive

- [ ] 4.1 Run broader backend and Dashboard verification for touched areas.
- [ ] 4.2 Run plan-implementation consistency audit and code-review-spec.
- [ ] 4.3 Archive the completed OpenSpec change after all tasks pass.
