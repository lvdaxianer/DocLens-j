package io.github.lvdaxianer.doclens.j.processing.infrastructure.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionRequest;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionResult;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.conversion.PdfPageImageRenderer;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.conversion.RenderedPageImage;
import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * PDF 图片提取器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class PdfImageDocumentExtractorTest {

    /**
     * PDF 页 OCR 必须使用注入的业务线程池，避免每个 PDF 自建默认线程池。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void extractUsesInjectedPageOcrExecutor() {
        RecordingExecutorService executor = new RecordingExecutorService();
        PdfImageDocumentExtractor extractor = new PdfImageDocumentExtractor(
                new FixedPdfPageImageRenderer(properties()),
                new FixedImageDocumentExtractor(),
                executor
        );

        DocumentTextExtractionResult result = extractor.extract(request());

        assertThat(executor.submittedTasks).isEqualTo(2);
        assertThat(result.finalText()).contains("page-1").contains("page-2");
    }

    /**
     * 创建测试提取请求。
     *
     * @return 文档文本提取请求
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private DocumentTextExtractionRequest request() {
        return new DocumentTextExtractionRequest(document(), "pdf".getBytes(), "stub_ocr");
    }

    /**
     * 创建测试文档任务。
     *
     * @return 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private DocumentJob document() {
        DocumentJobCreateRequest request = new DocumentJobCreateRequest("doc-test", "batch-test", "demo.pdf",
                DocumentType.PDF, 3, 2, "local://demo.pdf", "stub_ocr", Optional.empty(), JsonPayload.empty(), 0,
                OffsetDateTime.now());
        return DocumentJob.create(request);
    }

    /**
     * 创建测试配置。
     *
     * @return DocLens 配置
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private DocLensProperties properties() {
        return new DocLensProperties("target/test-storage", true, "worker-test",
                new DocLensProperties.CallbackProperties(1, 5),
                new DocLensProperties.AdapterProperties("stub_ocr"),
                new DocLensProperties.PaddleOcrProperties(false, "http://127.0.0.1:8080/ocr", 5, false),
                new DocLensProperties.ExtractionProperties(2),
                new DocLensProperties.PdfRenderProperties(36, "png"),
                new DocLensProperties.WordConversionProperties("soffice", 5));
    }

    /**
     * 固定返回两页图片的 PDF 渲染器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class FixedPdfPageImageRenderer extends PdfPageImageRenderer {

        /**
         * 创建固定页图片渲染器。
         *
         * @param properties DocLens 配置
         * @author lvdaxianerplus
         * @date 2026-06-08
         */
        FixedPdfPageImageRenderer(DocLensProperties properties) {
            super(properties);
        }

        @Override
        public List<RenderedPageImage> render(byte[] pdfContent) {
            return List.of(new RenderedPageImage(1, "one".getBytes()),
                    new RenderedPageImage(2, "two".getBytes()));
        }
    }

    /**
     * 固定返回页码文本的图片提取器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class FixedImageDocumentExtractor extends ImageDocumentExtractor {

        /**
         * 创建固定图片提取器。
         *
         * @author lvdaxianerplus
         * @date 2026-06-08
         */
        FixedImageDocumentExtractor() {
            super(null);
        }

        @Override
        public ImageOcrResult recognize(DocumentTextExtractionRequest request, int pageNo, byte[] imageContent) {
            return new ImageOcrResult(pageNo, Map.of("pageNo", pageNo),
                    List.of(Map.of("pageNo", pageNo, "text", "page-" + pageNo)), List.of(), 1D, List.of());
        }
    }

    /**
     * 记录提交任务次数的同步执行器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class RecordingExecutorService extends AbstractExecutorService {

        private int submittedTasks;
        private boolean isShutdown;

        @Override
        public void shutdown() {
            isShutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            isShutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return isShutdown;
        }

        @Override
        public boolean isTerminated() {
            return isShutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return isShutdown;
        }

        @Override
        public void execute(Runnable command) {
            submittedTasks++;
            command.run();
        }
    }
}
