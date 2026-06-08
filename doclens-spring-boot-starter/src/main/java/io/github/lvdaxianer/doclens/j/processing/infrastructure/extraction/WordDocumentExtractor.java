package io.github.lvdaxianer.doclens.j.processing.infrastructure.extraction;

import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionRequest;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionResult;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.conversion.WordToPdfConverter;

/**
 * Word 转 PDF 后复用 PDF 流程的提取器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class WordDocumentExtractor {

    private final WordToPdfConverter wordToPdfConverter;
    private final PdfImageDocumentExtractor pdfImageDocumentExtractor;

    /**
     * 创建 Word 文档提取器。
     *
     * @param wordToPdfConverter Word 转 PDF 转换器
     * @param pdfImageDocumentExtractor PDF 提取器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public WordDocumentExtractor(
            WordToPdfConverter wordToPdfConverter,
            PdfImageDocumentExtractor pdfImageDocumentExtractor
    ) {
        this.wordToPdfConverter = wordToPdfConverter;
        this.pdfImageDocumentExtractor = pdfImageDocumentExtractor;
    }

    /**
     * 提取 Word 最终纯文本。
     *
     * @param request 提取请求
     * @return 提取结果
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public DocumentTextExtractionResult extract(DocumentTextExtractionRequest request) {
        byte[] pdfContent = wordToPdfConverter.convert(request.document().fileName(), request.content());
        return pdfImageDocumentExtractor.extract(new DocumentTextExtractionRequest(request.document(), pdfContent,
                request.adapterKey()));
    }
}
