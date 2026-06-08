package io.github.lvdaxianer.doclens.j.processing.infrastructure.extraction;

import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionRequest;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionResult;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractor;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 按文档类型分发的默认文本提取器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class DefaultDocumentTextExtractor implements DocumentTextExtractor {

    private final PlainTextDocumentExtractor plainTextDocumentExtractor;
    private final ImageDocumentExtractor imageDocumentExtractor;
    private final PdfImageDocumentExtractor pdfImageDocumentExtractor;
    private final WordDocumentExtractor wordDocumentExtractor;
    private final Map<DocumentType, Function<DocumentTextExtractionRequest, DocumentTextExtractionResult>> strategies;

    /**
     * 创建默认文档文本提取器。
     *
     * @param plainTextDocumentExtractor 文本直通提取器
     * @param imageDocumentExtractor 图片 OCR 提取器
     * @param pdfImageDocumentExtractor PDF 提取器
     * @param wordDocumentExtractor Word 提取器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public DefaultDocumentTextExtractor(
            PlainTextDocumentExtractor plainTextDocumentExtractor,
            ImageDocumentExtractor imageDocumentExtractor,
            PdfImageDocumentExtractor pdfImageDocumentExtractor,
            WordDocumentExtractor wordDocumentExtractor
    ) {
        this.plainTextDocumentExtractor = plainTextDocumentExtractor;
        this.imageDocumentExtractor = imageDocumentExtractor;
        this.pdfImageDocumentExtractor = pdfImageDocumentExtractor;
        this.wordDocumentExtractor = wordDocumentExtractor;
        this.strategies = createStrategies();
    }

    /**
     * 按文档类型选择提取策略。
     *
     * @param request 提取请求
     * @return 提取结果
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Override
    public DocumentTextExtractionResult extract(DocumentTextExtractionRequest request) {
        return strategy(request.document().fileType()).apply(request);
    }

    /**
     * 创建文档类型到提取器的策略表。
     *
     * @return 文档提取策略表
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private Map<DocumentType, Function<DocumentTextExtractionRequest, DocumentTextExtractionResult>> createStrategies() {
        Map<DocumentType, Function<DocumentTextExtractionRequest, DocumentTextExtractionResult>> typeStrategies =
                new EnumMap<>(DocumentType.class);
        typeStrategies.put(DocumentType.MARKDOWN, plainTextDocumentExtractor::extract);
        typeStrategies.put(DocumentType.TEXT, plainTextDocumentExtractor::extract);
        typeStrategies.put(DocumentType.IMAGE, imageDocumentExtractor::extract);
        typeStrategies.put(DocumentType.PDF, pdfImageDocumentExtractor::extract);
        typeStrategies.put(DocumentType.WORD, wordDocumentExtractor::extract);
        return typeStrategies;
    }

    /**
     * 获取文档类型对应的提取策略。
     *
     * @param type 文档类型
     * @return 文档提取策略
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private Function<DocumentTextExtractionRequest, DocumentTextExtractionResult> strategy(DocumentType type) {
        Function<DocumentTextExtractionRequest, DocumentTextExtractionResult> strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("unsupported document type: " + type);
        } else {
            return strategy;
        }
    }
}
