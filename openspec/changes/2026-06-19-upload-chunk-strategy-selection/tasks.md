## 1. Upload preset selection

- [ ] 1.1 Add a failing dashboard test that proves the upload form shows the
  four chunking presets and defaults to the general preset.
- [ ] 1.2 Implement `UploadChunkStrategySelector` and wire it into
  `UploadDropzone` so the selected preset becomes part of the upload form state.
- [ ] 1.3 Extend the upload payload types and multipart builder so the selected
  preset is posted as `chunkStrategy`.
- [ ] 1.4 Re-run the focused dashboard tests and the dashboard build to confirm
  the selector does not break the existing upload flow.

## 2. Backend preset propagation and resolution

- [ ] 2.1 Add a failing backend test that proves `CreateBatchRequestMapper`
  preserves the `chunkStrategy` field from multipart upload requests.
- [ ] 2.2 Extend `CreateBatchRequest` and the upload request mapper so the new
  preset flows into the batch command without changing the existing upload
  metadata, callback, or OCR routing fields.
- [ ] 2.3 Add preset resolution classes in the chunking package and update
  `MarkdownChunker` to consume preset-aware defaults while preserving the
  current sliding-window behavior for the general preset.
- [ ] 2.4 Re-run the focused backend tests and the relevant broader backend slice
  to confirm the new preset changes the chunking defaults as intended.

## 3. Spec, verification, and closeout

- [ ] 3.1 Write the OpenSpec change for upload chunk strategy selection with the
  final preset list, default behavior, and non-goals captured explicitly.
- [ ] 3.2 Validate the OpenSpec change with `openspec validate --strict` and
  reconcile any gaps between the spec and the implementation plan.
- [ ] 3.3 Run the plan-implementation consistency audit and the code-review spec
  checklist on the final diff.
- [ ] 3.4 Commit the OpenSpec plan and supporting documentation as an atomic
  planning change once the checklist, tests, and review gates pass.
