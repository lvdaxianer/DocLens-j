## 1. Markdown result table rendering

- [x] 1.1 Add a failing Markdown preview utility test that proves pipe-table Markdown renders as a table while escaping unsafe cell content.
- [x] 1.2 Extend the safe Markdown preview renderer and result drawer styles so valid tables render readably and existing Markdown safety tests still pass.
- [x] 1.3 Re-run the focused Markdown utility tests and the result drawer UI test slice.

## 2. Dashboard upload preflight guard

- [x] 2.1 Add failing UI tests proving the upload form blocks submit and warns when the selected batch exceeds 30 files or 500 MB total.
- [x] 2.2 Add upload preflight validation helpers and wire them into `UploadDropzone` before the submit event is emitted.
- [x] 2.3 Re-run the focused upload UI tests and dashboard upload API tests.

## 3. Server upload limit fallback

- [ ] 3.1 Add failing server tests proving multipart upload config uses 500 MB and oversized multipart errors return structured HTTP 413 detail.
- [ ] 3.2 Align server multipart limits with the dashboard cap and add the structured 413 exception mapping.
- [ ] 3.3 Re-run the focused server tests for application config and upload error handling.

## 4. Final verification

- [ ] 4.1 Run dashboard utility tests, dashboard UI tests, dashboard build, and relevant server tests.
- [ ] 4.2 Run the final plan-implementation audit, archive the OpenSpec change, and commit the archive.
