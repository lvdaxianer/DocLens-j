package io.github.lvdaxianer.doclens.j.autoconfigure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingService;
import io.github.lvdaxianer.doclens.j.adapter.domain.DefaultAdapterRegistry;
import io.github.lvdaxianer.doclens.j.processing.application.PageImagePreparation;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractor;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.conversion.LibreOfficeWordToPdfConverter;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.conversion.PdfPageImageRenderer;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.conversion.WordToPdfConverter;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.extraction.DefaultPageImagePreparation;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.extraction.DefaultDocumentTextExtractor;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.extraction.ImageDocumentExtractor;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.extraction.PdfImageDocumentExtractor;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.extraction.PlainTextDocumentExtractor;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.extraction.WordDocumentExtractor;
import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 文档纯文本提取链路自动配置。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
@AutoConfiguration(after = DocLensAutoConfiguration.class)
public class DocLensExtractionAutoConfiguration {

    private static final int PAGE_OCR_QUEUE_CAPACITY = 100;
    private static final int THREAD_KEEP_ALIVE_SECONDS = 60;

    /**
     * 创建文本直通提取器。
     *
     * @return 文本直通提取器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnMissingBean
    PlainTextDocumentExtractor plainTextDocumentExtractor() {
        return new PlainTextDocumentExtractor();
    }

    /**
     * 创建图片 OCR 提取器。
     *
     * @param adapterRegistry OCR 适配器注册表
     * @param routingServiceProvider OCR 路由服务提供器
     * @return 图片 OCR 提取器
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Bean
    @ConditionalOnMissingBean
    ImageDocumentExtractor imageDocumentExtractor(
            DefaultAdapterRegistry adapterRegistry,
            ObjectProvider<OcrRoutingService> routingServiceProvider
    ) {
        return new ImageDocumentExtractor(adapterRegistry, routingServiceProvider.getIfAvailable());
    }

    /**
     * 创建 PDF 页图片渲染器。
     *
     * @param properties DocLens 配置
     * @return PDF 页图片渲染器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnMissingBean
    PdfPageImageRenderer pdfPageImageRenderer(DocLensProperties properties) {
        return new PdfPageImageRenderer(properties);
    }

    /**
     * 创建 PDF 页 OCR 专用线程池。
     *
     * @param properties DocLens 配置
     * @return PDF 页 OCR 线程池
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "docLensPageOcrExecutor")
    ExecutorService docLensPageOcrExecutor(DocLensProperties properties) {
        int concurrency = Math.max(1, properties.extraction().ocrConcurrency());
        return new ThreadPoolExecutor(concurrency, concurrency, THREAD_KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(PAGE_OCR_QUEUE_CAPACITY), pageOcrThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * 创建 PDF 页 OCR 线程工厂。
     *
     * @return PDF 页 OCR 线程工厂
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private ThreadFactory pageOcrThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("doclens-page-ocr-worker");
            thread.setDaemon(true);
            return thread;
        };
    }

    /**
     * 创建 PDF 图片 OCR 提取器。
     *
     * @param renderer PDF 页渲染器
     * @param imageDocumentExtractor 图片 OCR 提取器
     * @param pageOcrExecutor PDF 页 OCR 线程池
     * @return PDF 图片 OCR 提取器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnMissingBean
    PdfImageDocumentExtractor pdfImageDocumentExtractor(
            PdfPageImageRenderer renderer,
            ImageDocumentExtractor imageDocumentExtractor,
            @Qualifier("docLensPageOcrExecutor") ExecutorService pageOcrExecutor
    ) {
        return new PdfImageDocumentExtractor(renderer, imageDocumentExtractor, pageOcrExecutor);
    }

    /**
     * 创建 Word 转 PDF 转换器。
     *
     * @param properties DocLens 配置
     * @return Word 转 PDF 转换器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnMissingBean
    WordToPdfConverter wordToPdfConverter(DocLensProperties properties) {
        return new LibreOfficeWordToPdfConverter(properties);
    }

    /**
     * 创建页图片准备器。
     *
     * @param objectStorage 对象存储
     * @param pdfPageImageRenderer PDF 页图片渲染器
     * @param wordToPdfConverter Word 转 PDF 转换器
     * @return 页图片准备器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Bean
    @ConditionalOnMissingBean
    PageImagePreparation pageImagePreparation(
            ObjectStorage objectStorage,
            PdfPageImageRenderer pdfPageImageRenderer,
            WordToPdfConverter wordToPdfConverter
    ) {
        return new DefaultPageImagePreparation(objectStorage, pdfPageImageRenderer, wordToPdfConverter);
    }

    /**
     * 创建 Word 文档提取器。
     *
     * @param wordToPdfConverter Word 转 PDF 转换器
     * @param pdfImageDocumentExtractor PDF 图片 OCR 提取器
     * @return Word 文档提取器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnMissingBean
    WordDocumentExtractor wordDocumentExtractor(
            WordToPdfConverter wordToPdfConverter,
            PdfImageDocumentExtractor pdfImageDocumentExtractor
    ) {
        return new WordDocumentExtractor(wordToPdfConverter, pdfImageDocumentExtractor);
    }

    /**
     * 创建统一文档文本提取器。
     *
     * @param plainTextDocumentExtractor 文本直通提取器
     * @param imageDocumentExtractor 图片 OCR 提取器
     * @param pdfImageDocumentExtractor PDF 图片 OCR 提取器
     * @param wordDocumentExtractor Word 文档提取器
     * @return 统一文档文本提取器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnMissingBean
    DocumentTextExtractor documentTextExtractor(
            PlainTextDocumentExtractor plainTextDocumentExtractor,
            ImageDocumentExtractor imageDocumentExtractor,
            PdfImageDocumentExtractor pdfImageDocumentExtractor,
            WordDocumentExtractor wordDocumentExtractor
    ) {
        return new DefaultDocumentTextExtractor(plainTextDocumentExtractor, imageDocumentExtractor,
                pdfImageDocumentExtractor, wordDocumentExtractor);
    }
}
