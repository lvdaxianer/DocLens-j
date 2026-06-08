# Document Text Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert uploaded PDF, Word, Markdown, image, and TXT files into one final plain-text Markdown file and persist the same final text in the database.

**Architecture:** Add a document text extraction layer above OCR adapters. Plain text formats are read directly, images go through OCR, PDF pages are rendered to ordered images and OCRed concurrently, and Word documents are converted to PDF before reusing the PDF flow. OCR model strategy remains isolated behind `OcrAdapter`, while document-type orchestration lives in `DocumentTextExtractor`.

**Tech Stack:** Java 21, Maven, Spring Boot 3.5, Jackson, Java `HttpClient`, Apache PDFBox, local LibreOffice headless command for Word-to-PDF, JUnit 5, AssertJ, H2.

---

## Confirmed Requirements

- `markdown` / `md`: directly decode text and output it; no OCR and no document conversion.
- `txt`: directly decode text and output it; no OCR and no document conversion.
- `image`: call OCR once and use the recognized text as final plain text.
- `pdf`: render each page to an image, OCR page images concurrently, then merge recognized page text by page order.
- `word`: convert Word to PDF, then reuse the PDF pipeline.
- Multi-page output must be merged by original page order, even when OCR runs concurrently.
- Final disk output must be plain text only, stored as `[文件名称]_[uuid].md`.
- Final plain text must also be saved in the database.
- PaddleOCR native API is the first real image OCR strategy.
- Automated tests must not require the local PaddleOCR service or LibreOffice to be running.

## Current Context

- Existing project is Java 21 / Spring Boot / DDD with modules `doclens-api`, `doclens-core`, `doclens-spring-boot-starter`, and `doclens-server`.
- `DocumentType` currently only has `IMAGE`, `PDF`, and `WORD`; it must add `MARKDOWN` and `TEXT`.
- `CreateBatchUseCase.resolveFileType(...)` currently treats all non-PDF/non-image files as `WORD`.
- `BatchProcessingUseCase` currently calls `OcrAdapter.parse(DocumentJob)` and fabricates stub `pageText`/`layoutBlocks`.
- `ObjectStorage` already supports `writeBytes(...)` and `readBytes(...)`.
- `ocr_results` currently stores structured OCR JSON but has no explicit `final_text` or `markdown_storage_uri` columns.
- Parent Maven enforcer bans `spring-web`; PaddleOCR HTTP calls must use Java 21 `java.net.http.HttpClient`.

## File Structure

- Modify `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentType.java`
  - Add `MARKDOWN` and `TEXT`.
- Modify `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/application/CreateBatchUseCase.java`
  - Detect `.md`, `.markdown`, and `.txt`.
- Create `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/extraction/DocumentTextExtractor.java`
  - Top-level document-to-text strategy interface.
- Create `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/extraction/DocumentTextExtractionRequest.java`
  - Holds `DocumentJob`, file bytes, and selected OCR adapter key.
- Create `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/extraction/DocumentTextExtractionResult.java`
  - Holds final plain text, raw outputs, page text, layout blocks, images, warnings, confidence, and generated Markdown object key.
- Create `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrAdapter.java`
  - Change image OCR adapter contract to normalized image request/result.
- Create `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/ImageOcrRequest.java`
  - Holds document/page metadata and image bytes.
- Create `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/ImageOcrResult.java`
  - Holds one image/page OCR result and normalized blocks.
- Create `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrBlock.java`
  - Represents text, confidence, box, polygon, page number, and source.
- Modify `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/OcrResult.java`
  - Add `finalText` and `markdownStorageUri`.
- Modify `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingDependencies.java`
  - Add `ObjectStorage` and `DocumentTextExtractor`.
- Modify `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCase.java`
  - Read original bytes, extract final text, write `[文件名称]_[uuid].md`, and persist result.
- Modify `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/OcrQueryService.java`
  - Return `finalText` and `markdownStorageUri` in result view.
- Modify `doclens-spring-boot-starter/src/main/resources/db/migration/V1__doclens_ocr_schema.sql`
  - Do not edit for existing installations.
- Create `doclens-spring-boot-starter/src/main/resources/db/migration/V2__doclens_final_text.sql`
  - Add `final_text` and `markdown_storage_uri`.
- Modify `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/OcrResultEntity.java`
  - Add database fields.
- Modify `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusOcrResultRepository.java`
  - Map new fields.
- Create `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/DefaultDocumentTextExtractor.java`
  - Dispatch by `DocumentType`.
- Create `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/PlainTextDocumentExtractor.java`
  - Decode Markdown/TXT to plain text.
- Create `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/ImageDocumentExtractor.java`
  - Call selected `OcrAdapter` once.
- Create `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/PdfImageDocumentExtractor.java`
  - Render PDF pages to images and OCR concurrently.
- Create `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/WordDocumentExtractor.java`
  - Convert Word to PDF, then delegate to PDF extractor.
- Create `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/conversion/PdfPageImageRenderer.java`
  - PDFBox-backed page renderer.
- Create `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/conversion/LibreOfficeWordToPdfConverter.java`
  - Configurable local Word-to-PDF command wrapper.
- Modify `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/StubOcrAdapter.java`
  - Implement image OCR contract.
- Create `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNativeAdapter.java`
  - Implement PaddleOCR image OCR strategy.
- Create `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNativeClient.java`
  - Own PaddleOCR HTTP calls.
- Create `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNativeResponseMapper.java`
  - Map PaddleOCR JSON to image OCR result.
- Modify `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/shared/config/DocLensProperties.java`
  - Add adapter, PaddleOCR, rendering, conversion, and text-output properties.
- Modify `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringProperties.java`
  - Bind new properties.
- Modify `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensAutoConfiguration.java`
  - Register extraction/conversion/OCR beans.
- Modify `doclens-server/src/main/resources/application.yml`
  - Add local defaults.
- Update `README.md` and `README_EN.md`.

---

### Task 1: Add Text-Oriented Document Types

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentType.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/application/CreateBatchUseCase.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/ingestion/application/CreateBatchUseCaseTest.java`

- [ ] **Step 1: Write failing file-type tests**

Add tests that create batches with `note.md`, `readme.markdown`, `note.txt`, `scan.png`, `scan.pdf`, and `contract.docx`.

Assert created documents have:

```java
assertThat(markdownDocument.fileType()).isEqualTo(DocumentType.MARKDOWN);
assertThat(textDocument.fileType()).isEqualTo(DocumentType.TEXT);
assertThat(imageDocument.fileType()).isEqualTo(DocumentType.IMAGE);
assertThat(pdfDocument.fileType()).isEqualTo(DocumentType.PDF);
assertThat(wordDocument.fileType()).isEqualTo(DocumentType.WORD);
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run: `mvn -pl doclens-core -Dtest=CreateBatchUseCaseTest test`

Expected: FAIL because `MARKDOWN` and `TEXT` do not exist.

- [ ] **Step 3: Add enum values and extension detection**

Update `DocumentType`:

```java
public enum DocumentType {
    IMAGE,
    PDF,
    WORD,
    MARKDOWN,
    TEXT
}
```

Update `resolveFileType(String fileName)`:

```java
if (lowerName.endsWith(".pdf")) {
    return DocumentType.PDF;
} else if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg")
        || lowerName.endsWith(".jpeg") || lowerName.endsWith(".webp")
        || lowerName.endsWith(".tif") || lowerName.endsWith(".tiff")) {
    return DocumentType.IMAGE;
} else if (lowerName.endsWith(".md") || lowerName.endsWith(".markdown")) {
    return DocumentType.MARKDOWN;
} else if (lowerName.endsWith(".txt")) {
    return DocumentType.TEXT;
} else {
    return DocumentType.WORD;
}
```

- [ ] **Step 4: Run the focused test**

Run: `mvn -pl doclens-core -Dtest=CreateBatchUseCaseTest test`

Expected: PASS.

---

### Task 2: Define Unified Text Extraction and Image OCR Models

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrAdapter.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/ImageOcrRequest.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/ImageOcrResult.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrBlock.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/extraction/DocumentTextExtractor.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/extraction/DocumentTextExtractionRequest.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/extraction/DocumentTextExtractionResult.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/extraction/DocumentTextExtractionResultTest.java`

- [ ] **Step 1: Write failing merge-order test**

```java
@Test
void finalTextIsMergedByPageOrder() {
    ImageOcrResult pageTwo = ImageOcrResult.fromBlocks(2, Map.of("page", 2),
            List.of(new OcrBlock(2, "第二页", 0.90D, List.of(), List.of(), "stub_ocr")), List.of());
    ImageOcrResult pageOne = ImageOcrResult.fromBlocks(1, Map.of("page", 1),
            List.of(new OcrBlock(1, "第一页", 0.80D, List.of(), List.of(), "stub_ocr")), List.of());

    DocumentTextExtractionResult result = DocumentTextExtractionResult.fromPageResults(
            "doc_1", "demo.pdf", List.of(pageTwo, pageOne), List.of()
    );

    assertThat(result.finalText()).isEqualTo("第一页\n第二页");
    assertThat(result.pageText()).extracting(item -> item.get("pageNo")).containsExactly(1, 2);
    assertThat(result.confidence()).isEqualTo(0.85D);
}
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run: `mvn -pl doclens-core -Dtest=DocumentTextExtractionResultTest test`

Expected: compilation fails because extraction models do not exist.

- [ ] **Step 3: Implement OCR adapter boundary**

```java
public interface OcrAdapter {
    AdapterCapability capability();

    ImageOcrResult recognize(ImageOcrRequest request);
}
```

```java
public record ImageOcrRequest(
        String documentId,
        String fileName,
        int pageNo,
        byte[] imageContent,
        JsonPayload metadata
) {
}
```

```java
public record OcrBlock(
        int pageNo,
        String text,
        double confidence,
        List<Integer> box,
        List<List<Integer>> polygon,
        String source
) {
}
```

```java
public record ImageOcrResult(
        int pageNo,
        Map<String, Object> rawOutput,
        List<Map<String, Object>> pageText,
        List<Map<String, Object>> layoutBlocks,
        double confidence,
        List<String> warnings
) {
    public static ImageOcrResult fromBlocks(
            int pageNo,
            Map<String, Object> rawOutput,
            List<OcrBlock> blocks,
            List<String> warnings
    ) {
        String text = blocks.stream().map(OcrBlock::text).collect(Collectors.joining("\n"));
        double confidence = blocks.stream().mapToDouble(OcrBlock::confidence).average().orElse(0D);
        List<Map<String, Object>> pageText = List.of(Map.of("pageNo", pageNo, "text", text));
        List<Map<String, Object>> layoutBlocks = blocks.stream()
                .map(block -> Map.<String, Object>of(
                        "pageNo", block.pageNo(),
                        "type", "text",
                        "text", block.text(),
                        "confidence", block.confidence(),
                        "box", block.box(),
                        "polygon", block.polygon(),
                        "source", block.source()
                ))
                .toList();
        return new ImageOcrResult(pageNo, rawOutput, pageText, layoutBlocks, confidence, warnings);
    }
}
```

- [ ] **Step 4: Implement document extraction boundary**

```java
public interface DocumentTextExtractor {
    DocumentTextExtractionResult extract(DocumentTextExtractionRequest request);
}
```

```java
public record DocumentTextExtractionRequest(
        DocumentJob document,
        byte[] content,
        String adapterKey
) {
}
```

```java
public record DocumentTextExtractionResult(
        String finalText,
        Map<String, Object> rawOutput,
        Map<String, Object> structuredDocument,
        List<Map<String, Object>> pageText,
        List<Map<String, Object>> layoutBlocks,
        List<Map<String, Object>> tables,
        List<Map<String, Object>> images,
        double confidence,
        List<String> warnings
) {
    public static DocumentTextExtractionResult plainText(String documentId, String fileName, String text) {
        List<Map<String, Object>> pageText = List.of(Map.of("pageNo", 1, "text", text));
        return new DocumentTextExtractionResult(text, Map.of("source", "plain_text"),
                Map.of("documentId", documentId, "fileName", fileName, "pages", pageText),
                pageText, List.of(), List.of(), List.of(), 1D, List.of());
    }
}
```

Also add `fromPageResults(...)` that sorts by `ImageOcrResult.pageNo()` and joins non-blank page text with `\n`.

- [ ] **Step 5: Run the focused test**

Run: `mvn -pl doclens-core -Dtest=DocumentTextExtractionResultTest test`

Expected: PASS.

---

### Task 3: Persist Final Text and Markdown File URI

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/OcrResult.java`
- Create: `doclens-spring-boot-starter/src/main/resources/db/migration/V2__doclens_final_text.sql`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/OcrResultEntity.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusOcrResultRepository.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/OcrQueryService.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/starter/OcrResultPersistenceTest.java`

- [ ] **Step 1: Write failing persistence test**

Save an `OcrResult` with:

```java
String finalText = "最终纯文本";
String markdownStorageUri = "local://results/demo_550e8400-e29b-41d4-a716-446655440000.md";
```

Load it by document ID and assert:

```java
assertThat(found.finalText()).isEqualTo(finalText);
assertThat(found.markdownStorageUri()).isEqualTo(markdownStorageUri);
```

- [ ] **Step 2: Run focused persistence test and verify it fails**

Run: `mvn -pl doclens-spring-boot-starter -Dtest=OcrResultPersistenceTest test`

Expected: FAIL because fields and columns do not exist.

- [ ] **Step 3: Extend `OcrResult`**

Add fields after `documentId`:

```java
String finalText,
String markdownStorageUri,
```

- [ ] **Step 4: Add migration**

Create `V2__doclens_final_text.sql`:

```sql
ALTER TABLE ocr_results
    ADD COLUMN IF NOT EXISTS final_text TEXT NOT NULL DEFAULT '';

ALTER TABLE ocr_results
    ADD COLUMN IF NOT EXISTS markdown_storage_uri VARCHAR(2048) NOT NULL DEFAULT '';
```

- [ ] **Step 5: Map entity and repository fields**

Add `finalText` and `markdownStorageUri` to `OcrResultEntity`, and map both directions in `MybatisPlusOcrResultRepository`.

- [ ] **Step 6: Expose fields in query result**

In `OcrQueryService.getDocumentResult(...)`, include:

```java
Map.entry("finalText", result.finalText()),
Map.entry("markdownStorageUri", result.markdownStorageUri()),
```

- [ ] **Step 7: Run persistence test**

Run: `mvn -pl doclens-spring-boot-starter -Dtest=OcrResultPersistenceTest test`

Expected: PASS.

---

### Task 4: Write Final Plain Text to Disk

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCase.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingDependencies.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownResultNamer.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownResultNamerTest.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseTest.java`

- [ ] **Step 1: Write failing filename test**

```java
@Test
void buildsPlainMarkdownResultNameFromOriginalFileNameAndUuid() {
    String name = MarkdownResultNamer.markdownFileName("合同.pdf", UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

    assertThat(name).isEqualTo("合同_550e8400-e29b-41d4-a716-446655440000.md");
}
```

- [ ] **Step 2: Implement filename helper**

Rules:

- Remove the final extension from the original file name.
- Replace `/`, `\`, and control characters with `_`.
- Preserve Chinese characters.
- Append `_` + UUID + `.md`.

- [ ] **Step 3: Write failing processing test**

Use fake `DocumentTextExtractor` returning `finalText = "最终纯文本"`.

Assert `ObjectStorage.writeBytes(...)` receives:

```java
objectKey.startsWith("results/")
new String(content, StandardCharsets.UTF_8).equals("最终纯文本")
objectKey.endsWith(".md")
```

Assert saved `OcrResult.finalText()` is `最终纯文本` and `markdownStorageUri()` is the URI returned by object storage.

- [ ] **Step 4: Wire extraction and markdown persistence**

In `BatchProcessingUseCase.buildResult(...)`:

```java
byte[] content = objectStorage.readBytes(document.storageUri());
DocumentTextExtractionResult extracted = documentTextExtractor.extract(
        new DocumentTextExtractionRequest(document, content, document.adapterName())
);
String markdownObjectKey = "results/%s/%s/%s".formatted(
        document.batchId(),
        document.documentId(),
        MarkdownResultNamer.markdownFileName(document.fileName(), UUID.randomUUID())
);
String markdownStorageUri = objectStorage.writeBytes(
        markdownObjectKey,
        extracted.finalText().getBytes(StandardCharsets.UTF_8)
);
return new OcrResult(resultId, document.documentId(), extracted.finalText(), markdownStorageUri,
        extracted.rawOutput(), extracted.structuredDocument(), extracted.pageText(), extracted.layoutBlocks(),
        extracted.tables(), extracted.images(), extracted.confidence(), extracted.warnings(), OffsetDateTime.now());
```

- [ ] **Step 5: Run focused tests**

Run: `mvn -pl doclens-core -Dtest=MarkdownResultNamerTest,BatchProcessingUseCaseTest test`

Expected: PASS.

---

### Task 5: Implement Plain Text and Image Extraction

**Files:**
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/PlainTextDocumentExtractor.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/ImageDocumentExtractor.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/DefaultDocumentTextExtractor.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensAutoConfiguration.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/PlainTextDocumentExtractorTest.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/ImageDocumentExtractorTest.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/DefaultDocumentTextExtractorTest.java`

- [ ] **Step 1: Write plain text tests**

For Markdown bytes `"# 标题\n正文"` assert final text is exactly:

```text
# 标题
正文
```

For TXT bytes `"hello"` assert final text is exactly `hello`.

- [ ] **Step 2: Implement `PlainTextDocumentExtractor`**

Decode bytes using UTF-8:

```java
String text = new String(request.content(), StandardCharsets.UTF_8);
return DocumentTextExtractionResult.plainText(request.document().documentId(), request.document().fileName(), text);
```

- [ ] **Step 3: Write image extraction test**

Fake `OcrAdapter` returns page text `图片文字`.

Assert final text is `图片文字` and OCR adapter receives the original image bytes with `pageNo = 1`.

- [ ] **Step 4: Implement `ImageDocumentExtractor`**

Find the OCR adapter by `adapterKey`, call:

```java
adapter.recognize(new ImageOcrRequest(document.documentId(), document.fileName(), 1, request.content(), document.metadata()))
```

Convert the `ImageOcrResult` to `DocumentTextExtractionResult.fromPageResults(...)`.

- [ ] **Step 5: Implement dispatcher**

`DefaultDocumentTextExtractor` dispatches:

```java
MARKDOWN, TEXT -> plainTextDocumentExtractor
IMAGE -> imageDocumentExtractor
PDF -> pdfImageDocumentExtractor
WORD -> wordDocumentExtractor
```

For unsupported types, throw `IllegalArgumentException("unsupported document type: " + type)`.

- [ ] **Step 6: Run focused extraction tests**

Run: `mvn -pl doclens-spring-boot-starter -Dtest=PlainTextDocumentExtractorTest,ImageDocumentExtractorTest,DefaultDocumentTextExtractorTest test`

Expected: PASS.

---

### Task 6: Implement PDF Page Rendering and Concurrent OCR

**Files:**
- Modify: `doclens-spring-boot-starter/pom.xml`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/conversion/PdfPageImageRenderer.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/PdfImageDocumentExtractor.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/shared/config/DocLensProperties.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringProperties.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/PdfImageDocumentExtractorTest.java`

- [ ] **Step 1: Add PDFBox dependency**

Add to `doclens-spring-boot-starter/pom.xml`:

```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.5</version>
</dependency>
```

- [ ] **Step 2: Add render/concurrency properties**

Core properties:

```java
public record PdfRenderProperties(int dpi, String imageFormat) {
}

public record ExtractionProperties(int ocrConcurrency) {
}
```

Default values:

```yaml
doclens:
  extraction:
    ocr-concurrency: 4
  pdf-render:
    dpi: 200
    image-format: png
```

- [ ] **Step 3: Write concurrent order test**

Fake renderer returns three page images with page numbers 1, 2, 3.

Fake OCR adapter deliberately returns page 3 first, page 1 second, page 2 third.

Assert final text:

```text
第一页
第二页
第三页
```

- [ ] **Step 4: Implement `PdfPageImageRenderer`**

Use PDFBox `Loader.loadPDF(byte[])`, `PDFRenderer`, and `ImageIO.write(...)` to render each page to PNG bytes.

Return records:

```java
public record RenderedPageImage(int pageNo, byte[] content) {
}
```

- [ ] **Step 5: Implement `PdfImageDocumentExtractor`**

Use fixed concurrency from properties:

```java
ExecutorService executor = Executors.newFixedThreadPool(properties.extraction().ocrConcurrency());
```

Submit one OCR task per rendered page. Collect all futures, then sort by `ImageOcrResult.pageNo()` before merging.

Always call `executor.shutdown()` in `finally`.

- [ ] **Step 6: Run focused PDF test**

Run: `mvn -pl doclens-spring-boot-starter -Dtest=PdfImageDocumentExtractorTest test`

Expected: PASS.

---

### Task 7: Implement Word-to-PDF Delegation

**Files:**
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/conversion/WordToPdfConverter.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/conversion/LibreOfficeWordToPdfConverter.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/WordDocumentExtractor.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/shared/config/DocLensProperties.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringProperties.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/WordDocumentExtractorTest.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/conversion/LibreOfficeWordToPdfConverterTest.java`

- [ ] **Step 1: Add Word conversion properties**

```java
public record WordConversionProperties(String command, int timeoutSeconds) {
}
```

Default local YAML:

```yaml
doclens:
  word-conversion:
    command: soffice
    timeout-seconds: 60
```

- [ ] **Step 2: Write delegation test**

Fake `WordToPdfConverter` returns PDF bytes `"%PDF-demo"`.

Fake PDF extractor returns final text `Word 转出来的文字`.

Assert `WordDocumentExtractor.extract(...)` returns final text `Word 转出来的文字`.

- [ ] **Step 3: Implement `WordDocumentExtractor`**

Convert:

```java
byte[] pdfBytes = wordToPdfConverter.convert(request.document().fileName(), request.content());
```

Then create a request equivalent to the original document but with PDF bytes and delegate to `PdfImageDocumentExtractor`.

- [ ] **Step 4: Implement LibreOffice converter**

Use temp directory, write original Word bytes, run:

```bash
soffice --headless --convert-to pdf --outdir <temp-dir> <input-file>
```

On timeout, destroy the process and throw `IllegalStateException("Word to PDF conversion timed out")`.

On non-zero exit, throw `IllegalStateException("Word to PDF conversion failed")`.

Do not log file content.

- [ ] **Step 5: Run focused Word tests**

Run: `mvn -pl doclens-spring-boot-starter -Dtest=WordDocumentExtractorTest,LibreOfficeWordToPdfConverterTest test`

Expected: PASS. The LibreOffice process test should use a fake command script, not a real `soffice` binary.

---

### Task 8: Implement PaddleOCR Native Image Adapter

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/StubOcrAdapter.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNativeAdapter.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNativeClient.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNativeResponseMapper.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/shared/config/DocLensProperties.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringProperties.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNativeResponseMapperTest.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNativeClientTest.java`

- [ ] **Step 1: Add PaddleOCR properties**

```java
public record PaddleOcrProperties(boolean enabled, String endpoint, int timeoutSeconds, boolean visualize) {
}
```

Default local YAML:

```yaml
doclens:
  paddle-ocr:
    enabled: true
    endpoint: http://127.0.0.1:8080/ocr
    timeout-seconds: 30
    visualize: false
```

- [ ] **Step 2: Write mapper test with supplied response**

Use:

```json
{
  "errorCode": 0,
  "errorMsg": "Success",
  "result": {
    "ocrResults": [
      {
        "prunedResult": {
          "rec_texts": ["识别文本"],
          "rec_scores": [0.95],
          "rec_boxes": [[0, 78, 139, 91]],
          "rec_polys": [],
          "dt_polys": []
        }
      }
    ]
  }
}
```

Assert page text is `识别文本`, block confidence is `0.95D`, and raw output keeps `errorCode = 0`.

- [ ] **Step 3: Write HTTP client test**

Use JDK `HttpServer`, call the client with image bytes `{1, 2, 3}`, and assert captured JSON contains:

```json
"file":"AQID"
"fileType":1
"visualize":false
```

- [ ] **Step 4: Implement client and mapper**

Rules:

- Native API path is configured as full endpoint URL.
- Request body is JSON with `file`, `fileType`, and `visualize`.
- For this adapter, always send `fileType = 1` because the document pipeline sends images only.
- `errorCode != 0` throws `IllegalStateException("PaddleOCR native API failed: " + errorMsg)`.
- Missing scores become `0D`; missing boxes and polygons become empty lists.

- [ ] **Step 5: Update Stub adapter**

`StubOcrAdapter.recognize(...)` returns one block:

```java
new OcrBlock(request.pageNo(), "Stub OCR text for " + request.fileName(),
        DocLensConstants.STUB_CONFIDENCE, List.of(), List.of(), "stub_ocr")
```

- [ ] **Step 6: Run focused adapter tests**

Run: `mvn -pl doclens-spring-boot-starter -Dtest=PaddleOcrNativeResponseMapperTest,PaddleOcrNativeClientTest test`

Expected: PASS.

---

### Task 9: Add Adapter Strategy and Auto-Configuration

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/shared/config/DocLensProperties.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringProperties.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensAutoConfiguration.java`
- Modify: `doclens-server/src/main/resources/application.yml`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/starter/AdapterRegistrationTest.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/starter/DocLensStarterEmbeddedTest.java`

- [ ] **Step 1: Add adapter default-key property**

```java
public record AdapterProperties(String defaultKey) {
}
```

Default:

```yaml
doclens:
  adapter:
    default-key: paddle_ocr
```

- [ ] **Step 2: Define strategy order**

Use selected adapter key in this order:

1. `CreateBatchCommand.adapterOverride()`
2. `doclens.adapter.default-key`
3. fallback `paddle_ocr`

- [ ] **Step 3: Register beans**

Auto-configuration registers:

- `StubOcrAdapter` always, with key `stub_ocr`.
- `PaddleOcrNativeAdapter` only when `doclens.paddle-ocr.enabled=true`.
- `DefaultDocumentTextExtractor`.
- `PlainTextDocumentExtractor`.
- `ImageDocumentExtractor`.
- `PdfImageDocumentExtractor`.
- `WordDocumentExtractor`.
- `PdfPageImageRenderer`.
- `LibreOfficeWordToPdfConverter`.

- [ ] **Step 4: Keep automated tests local-service independent**

In tests that process documents automatically, set:

```java
registry.add("doclens.adapter.default-key", () -> "stub_ocr");
registry.add("doclens.paddle-ocr.enabled", () -> "false");
```

- [ ] **Step 5: Run focused starter tests**

Run: `mvn -pl doclens-spring-boot-starter -Dtest=AdapterRegistrationTest,DocLensStarterEmbeddedTest test`

Expected: PASS.

---

### Task 10: Update Public Contract and Documentation

**Files:**
- Modify: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/DocLensOcrApiContractTest.java`
- Modify: `README.md`
- Modify: `README_EN.md`

- [ ] **Step 1: Update contract assertions**

Assert `GET /api/v1/documents/{documentId}/result` contains:

```java
.andExpect(jsonPath("$.result.finalText").value("Stub OCR text for a.pdf"))
.andExpect(jsonPath("$.result.markdownStorageUri").isString())
```

For Markdown upload contract, assert `finalText` equals the uploaded Markdown content.

- [ ] **Step 2: Document pipeline behavior**

README must include:

```text
Markdown/TXT: direct text extraction.
Image: image OCR.
PDF: render pages to images, OCR concurrently, merge by page order.
Word: convert to PDF, then use the PDF flow.
Final output: plain text saved to database and to [文件名称]_[uuid].md.
```

- [ ] **Step 3: Document local PaddleOCR startup**

```bash
cd /Users/lvdaxianer/cache/soft/paddleocr-api
./start-native.sh
```

Do not recommend `./start.sh` port 8800 as the default adapter target.

- [ ] **Step 4: Document local config**

```yaml
doclens:
  adapter:
    default-key: paddle_ocr
  paddle-ocr:
    enabled: true
    endpoint: http://127.0.0.1:8080/ocr
    timeout-seconds: 30
    visualize: false
  extraction:
    ocr-concurrency: 4
  pdf-render:
    dpi: 200
    image-format: png
  word-conversion:
    command: soffice
    timeout-seconds: 60
```

- [ ] **Step 5: Run full verification**

Run: `mvn test`

Expected: PASS.

---

## Manual Integration Check

Start PaddleOCR:

```bash
cd /Users/lvdaxianer/cache/soft/paddleocr-api
./start-native.sh
```

Start DocLens:

```bash
mvn -pl doclens-server spring-boot:run
```

Upload an image:

```bash
curl -X POST http://127.0.0.1:8080/api/v1/batches \
  -F 'files=@/path/to/image.png' \
  -F 'metadata={"source":"manual-paddleocr"}'
```

Expected:

- Result response has `result.finalText`.
- Result response has `result.markdownStorageUri`.
- Disk contains one `.md` file named like `[文件名称]_[uuid].md`.
- The `.md` file content is exactly the final plain text, with no page headings or metadata wrapper.
- Logs do not include uploaded file bytes or base64 payloads.

## Risk Notes

- Word-to-PDF depends on a local LibreOffice-compatible command. Automated tests must use a fake command script.
- PDF rendering and OCR are memory-heavy. Keep upload size limits and use bounded concurrency.
- OCR page results return out of order under concurrency; all merge logic must sort by page number.
- Plain Markdown and TXT are treated as text payloads. If future uploads include non-UTF-8 text, add charset detection as a separate task.
- PaddleOCR receives images only in this design. PDF direct OCR is intentionally not the default path because the requirement says PDFs must be converted to images first.

## Self-Review

- Spec coverage: all requested upload types, direct text handling, image OCR, PDF image splitting, Word-to-PDF, concurrent OCR, ordered merge, database persistence, and `[文件名称]_[uuid].md` disk persistence are covered.
- Placeholder scan: no step uses `TBD`, `TODO`, or unspecified implementation.
- Type consistency: image OCR models are separate from document extraction models, and extraction results feed `OcrResult`.
- Scope check: this plan is one coherent document-to-text pipeline; it does not add async distributed workers, non-UTF-8 charset detection, or direct PDF OCR.
