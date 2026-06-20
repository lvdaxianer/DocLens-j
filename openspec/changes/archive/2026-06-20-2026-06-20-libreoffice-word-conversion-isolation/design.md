## Context

`LibreOfficeWordToPdfConverter.convert()` writes the uploaded Word bytes into a per-call temp directory, starts `/opt/homebrew/bin/soffice --headless --convert-to pdf --outdir <temp> <input>`, waits for the process, then expects `<input-base>.pdf` in the same directory.

The upload pipeline processes documents in a batch through `BatchProcessingUseCase` using the document processing executor. Word documents go through `DefaultPageImagePreparation`, which invokes the converter before page task creation. When several Word documents are prepared at the same time, each converter call has a distinct input/output temp directory, but all LibreOffice processes still share the same default user installation.

## Goals / Non-Goals

**Goals:**
- Isolate each LibreOffice conversion with a per-conversion `UserInstallation` profile.
- Preserve concurrent document processing instead of serializing all Word conversions.
- Capture stdout/stderr from the LibreOffice process and include a concise diagnostic in thrown conversion exceptions.
- Add regression coverage that inspects the generated command and failure message behavior without requiring a real LibreOffice installation.

**Non-Goals:**
- Do not change OCR node routing, page task worker behavior, or PDF rendering.
- Do not introduce a new external conversion library.
- Do not change Dashboard response fields or status labels in this change.
- Do not retry failed conversions automatically; this change addresses profile contention as the root cause.

## Decisions

- Add `-env:UserInstallation=file://<temp-profile-dir>` to every LibreOffice process invocation. The profile directory lives under the existing per-conversion temp directory so cleanup remains local to the conversion.
- Keep the existing per-conversion input/output temp directory model.
- Capture merged process output through `redirectErrorStream(true)` and read it before evaluating the exit code or missing output.
- Surface failure messages as `Word to PDF conversion failed: <diagnostic>` or `Word to PDF conversion did not produce PDF output: <diagnostic>` when process output exists.
- Test command construction by injecting a process-launching seam around `ProcessBuilder`, keeping production behavior unchanged for callers.

## Data Flow

1. A Word document enters page image preparation.
2. The converter creates a unique temp directory and a unique LibreOffice profile directory.
3. The converter starts LibreOffice with the isolated `UserInstallation` profile.
4. The converter captures process output and waits for completion.
5. On success, the converter reads the generated PDF from the temp output directory.
6. On failure or missing output, the converter throws an exception that includes the captured LibreOffice diagnostic.
7. The existing cleanup removes the input, output, and profile temp files.

## Risks / Trade-offs

- A per-conversion profile adds small filesystem overhead, but avoids shared LibreOffice state and keeps document-level parallelism.
- Some LibreOffice diagnostics can be verbose. The implementation should trim output and keep only a bounded message for exceptions.
- The tests should not depend on `/opt/homebrew/bin/soffice` being installed, so they must validate process construction with a fake launcher.

## Testing

- Add a failing unit test for `LibreOfficeWordToPdfConverter` proving the process command includes an isolated `-env:UserInstallation=file://...` argument for each conversion.
- Add a failing unit test proving non-zero conversion output is included in the thrown exception message.
- Run the focused converter tests.
- Run the broader starter test slice that covers extraction/conversion infrastructure.
