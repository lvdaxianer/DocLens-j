package io.github.lvdaxianer.doclens.j.processing.infrastructure.extraction;

import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionRequest;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionResult;
import java.nio.charset.StandardCharsets;

/**
 * Markdown 和 TXT 直通提取器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class PlainTextDocumentExtractor {

    /**
     * 直接将文本文件解码为最终纯文本。
     *
     * @param request 提取请求
     * @return 提取结果
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public DocumentTextExtractionResult extract(DocumentTextExtractionRequest request) {
        String text = new String(request.content(), StandardCharsets.UTF_8);
        return DocumentTextExtractionResult.plainText(request.document().documentId(), request.document().fileName(), text);
    }
}
