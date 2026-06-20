## 1. Isolate LibreOffice conversion runtime

- [x] 1.1 Add failing converter tests proving each conversion command uses an isolated LibreOffice `UserInstallation` profile and preserves useful process diagnostics on failure.
- [x] 1.2 Implement isolated LibreOffice profile command construction and bounded process-output diagnostics in `LibreOfficeWordToPdfConverter`.
- [ ] 1.3 Re-run the focused converter tests and the broader extraction/conversion test slice.
