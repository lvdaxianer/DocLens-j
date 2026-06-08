package io.github.lvdaxianer.doclens.j.shared.config;

/**
 * DocLens core 运行时配置。
 *
 * @param storageRoot 本地对象存储根目录
 * @param autoProcessOnUpload 上传请求是否触发进程内 Worker 执行
 * @param workerId 用于后续任务获取的本地 Worker 标识
 * @param callback 回调投递配置
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record DocLensProperties(
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

    /**
     * 回调重试与超时配置。
     *
     * @param maxRetries 最大回调重试次数
     * @param timeoutSeconds 回调请求超时时间，单位秒
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public record CallbackProperties(int maxRetries, int timeoutSeconds) {
    }

    /**
     * OCR 适配器配置。
     *
     * @param defaultKey 默认适配器键
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public record AdapterProperties(String defaultKey) {
    }

    /**
     * PaddleOCR 原生 API 配置。
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
     * 提取并发配置。
     *
     * @param ocrConcurrency OCR 并发数
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public record ExtractionProperties(int ocrConcurrency) {
    }

    /**
     * PDF 渲染配置。
     *
     * @param dpi 渲染 DPI
     * @param imageFormat 图片格式
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public record PdfRenderProperties(int dpi, String imageFormat) {
    }

    /**
     * Word 转 PDF 配置。
     *
     * @param command LibreOffice 命令
     * @param timeoutSeconds 超时秒数
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public record WordConversionProperties(String command, int timeoutSeconds) {
    }
}
