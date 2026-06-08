package io.github.lvdaxianer.doclens.j.shared.config;

/**
 * DocLens core 运行时配置。
 *
 * @param storageRoot 本地对象存储根目录
 * @param autoProcessOnUpload 上传请求是否触发进程内 Worker 执行
 * @param workerId 用于后续任务获取的本地 Worker 标识
 * @param callback 回调投递配置
 * @param adapter 适配器配置
 * @param paddleOcr PaddleOCR 配置
 * @param ocrHealth OCR 健康检查配置
 * @param threadPools 线程池隔离配置
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
        OcrHealthProperties ocrHealth,
        ExtractionProperties extraction,
        PdfRenderProperties pdfRender,
        WordConversionProperties wordConversion,
        ThreadPoolsProperties threadPools
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
     * OCR 健康检查配置。
     *
     * @param healthFailureThreshold 健康检查失败摘除阈值
     * @param recoverySuccessThreshold 恢复成功阈值
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public record OcrHealthProperties(int healthFailureThreshold, int recoverySuccessThreshold) {
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

    /**
     * DocLens 线程池隔离配置。
     *
     * @param documentProcessingThreadPool 文档处理线程池
     * @param ocrRequestThreadPool OCR 请求线程池
     * @param ocrHealthThreadPool OCR 健康检查线程池
     * @param callbackThreadPool 回调线程池
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public record ThreadPoolsProperties(
            ThreadPoolProperties documentProcessingThreadPool,
            ThreadPoolProperties ocrRequestThreadPool,
            ThreadPoolProperties ocrHealthThreadPool,
            ThreadPoolProperties callbackThreadPool
    ) {
    }

    /**
     * 单个业务线程池配置。
     *
     * @param coreSize 核心线程数
     * @param maxSize 最大线程数
     * @param queueCapacity 队列容量
     * @param keepAliveSeconds 空闲线程保活秒数
     * @param threadNamePrefix 线程名前缀
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public record ThreadPoolProperties(
            int coreSize,
            int maxSize,
            int queueCapacity,
            int keepAliveSeconds,
            String threadNamePrefix
    ) {
    }
}
