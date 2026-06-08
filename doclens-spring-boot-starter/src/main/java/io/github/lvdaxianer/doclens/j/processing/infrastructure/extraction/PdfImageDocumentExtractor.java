package io.github.lvdaxianer.doclens.j.processing.infrastructure.extraction;

import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionRequest;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionResult;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.conversion.PdfPageImageRenderer;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.conversion.RenderedPageImage;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PDF 转页图片并并发 OCR 的提取器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class PdfImageDocumentExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfImageDocumentExtractor.class);

    private final PdfPageImageRenderer renderer;
    private final ImageDocumentExtractor imageDocumentExtractor;
    private final ExecutorService pageOcrExecutor;

    /**
     * 创建 PDF 图片 OCR 提取器。
     *
     * @param renderer PDF 页渲染器
     * @param imageDocumentExtractor 图片 OCR 提取器
     * @param pageOcrExecutor PDF 页 OCR 线程池
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public PdfImageDocumentExtractor(
            PdfPageImageRenderer renderer,
            ImageDocumentExtractor imageDocumentExtractor,
            ExecutorService pageOcrExecutor
    ) {
        this.renderer = renderer;
        this.imageDocumentExtractor = imageDocumentExtractor;
        this.pageOcrExecutor = pageOcrExecutor;
    }

    /**
     * 提取 PDF 最终纯文本。
     *
     * @param request 提取请求
     * @return 提取结果
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public DocumentTextExtractionResult extract(DocumentTextExtractionRequest request) {
        List<RenderedPageImage> images = renderer.render(request.content());
        LOGGER.info("[OCR处理] PDF页OCR开始 documentId={}, pageCount={}", request.document().documentId(),
                images.size());
        try {
            List<Callable<ImageOcrResult>> tasks = images.stream()
                    .map(image -> (Callable<ImageOcrResult>) () -> imageDocumentExtractor.recognize(request,
                            image.pageNo(), image.content()))
                    .toList();
            List<ImageOcrResult> results = pageOcrExecutor.invokeAll(tasks).stream()
                    .map(this::pageResult)
                    .toList();
            LOGGER.info("[OCR处理] PDF页OCR完成 documentId={}, pageCount={}", request.document().documentId(),
                    results.size());
            return DocumentTextExtractionResult.fromPageResults(request.document().documentId(),
                    request.document().fileName(), results, List.of());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("PDF OCR interrupted", ex);
        }
    }

    /**
     * 获取单页 OCR 结果并保留异常链。
     *
     * @param future 单页 OCR 异步结果
     * @return 单页 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private ImageOcrResult pageResult(Future<ImageOcrResult> future) {
        try {
            return future.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("PDF page OCR interrupted", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("failed to OCR rendered PDF page", ex);
        }
    }
}
