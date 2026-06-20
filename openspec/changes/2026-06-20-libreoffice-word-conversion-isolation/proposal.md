## Why

Batch uploads can process multiple Word documents concurrently. The current LibreOffice conversion command uses the default LibreOffice user profile, so parallel `soffice --headless --convert-to pdf` processes can contend for the same profile or single-instance channel. In the observed batch, several Word documents failed within 1-3 seconds with `Word to PDF conversion failed` or `Word to PDF conversion did not produce PDF output`.

## What Changes

Run each Word-to-PDF conversion with an isolated LibreOffice `UserInstallation` profile under the conversion temp directory, and include captured LibreOffice process output in conversion failure messages.

## Impact

The change affects Word document preparation only. PDF, image, text, Markdown ingestion, page rendering, OCR routing, and Dashboard contracts remain unchanged. Successful Word conversions should keep their current output behavior while concurrent Word conversions no longer interfere through the shared LibreOffice runtime profile.
