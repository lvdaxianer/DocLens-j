package io.github.lvdaxianer.doclens.j.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定到 Spring 的 DocLens 配置属性。
 *
 * @param storageRoot 存储根目录
 * @param autoProcessOnUpload 自动处理标志
 * @param workerId Worker 标识
 * @param callback 回调配置
 * @param adapter 适配器配置
 * @param paddleOcr PaddleOCR 配置
 * @param extraction 提取配置
 * @param pdfRender PDF 渲染配置
 * @param wordConversion Word 转 PDF 配置
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@ConfigurationProperties(prefix = "doclens")
public record DocLensSpringProperties(
        String storageRoot,
        boolean autoProcessOnUpload,
        String workerId,
        CallbackProperties callback,
        AdapterProperties adapter,
        PaddleOcrProperties paddleOcr,
        ExtractionProperties extraction,
        PdfRenderProperties pdfRender,
        WordConversionProperties wordConversion
) {
    private static final String DEFAULT_PADDLE_OCR_ENDPOINT = "http://10.100.30.215:8080/ocr";

    public DocLensSpringProperties {
        callback = callback == null ? new CallbackProperties(3, 10) : callback;
        adapter = adapter == null ? new AdapterProperties("paddle_ocr") : adapter;
        paddleOcr = paddleOcr == null
                ? new PaddleOcrProperties(true, DEFAULT_PADDLE_OCR_ENDPOINT, 600, false)
                : paddleOcr;
        extraction = extraction == null ? new ExtractionProperties(1) : extraction;
        pdfRender = pdfRender == null ? new PdfRenderProperties(36, "png") : pdfRender;
        wordConversion = wordConversion == null ? new WordConversionProperties("soffice", 60) : wordConversion;
    }

    /**
     * 回调重试与超时属性。
     *
     * @param maxRetries 最大重试次数
     * @param timeoutSeconds 超时秒数
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public record CallbackProperties(int maxRetries, int timeoutSeconds) {
    }

    /**
     * 适配器属性。
     *
     * @param defaultKey 默认适配器键
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public record AdapterProperties(String defaultKey) {
    }

    /**
     * PaddleOCR 属性。
     *
     * @param enabled 是否启用
     * @param endpoint 接口地址
     * @param timeoutSeconds 超时秒数
     * @param visualize 是否请求可视化
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public record PaddleOcrProperties(boolean enabled, String endpoint, int timeoutSeconds, boolean visualize) {
    }

    /**
     * 提取属性。
     *
     * @param ocrConcurrency OCR 并发数
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public record ExtractionProperties(int ocrConcurrency) {
    }

    /**
     * PDF 渲染属性。
     *
     * @param dpi 渲染 DPI
     * @param imageFormat 图片格式
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public record PdfRenderProperties(int dpi, String imageFormat) {
    }

    /**
     * Word 转 PDF 属性。
     *
     * @param command 转换命令
     * @param timeoutSeconds 超时秒数
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public record WordConversionProperties(String command, int timeoutSeconds) {
    }
}
