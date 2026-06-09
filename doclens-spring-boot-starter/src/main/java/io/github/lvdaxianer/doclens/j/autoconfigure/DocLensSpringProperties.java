package io.github.lvdaxianer.doclens.j.autoconfigure;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutingMode;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定到 Spring 的 DocLens 配置属性。
 *
 * @param storageRoot 存储根目录
 * @param autoProcessOnUpload 自动处理标志
 * @param workerId Worker 标识
 * @param callback 回调配置
 * @param adapter 适配器配置
 * @param ocr OCR 路由配置
 * @param paddleOcr PaddleOCR 配置
 * @param ocrHealth OCR 健康检查配置
 * @param extraction 提取配置
 * @param pdfRender PDF 渲染配置
 * @param wordConversion Word 转 PDF 配置
 * @param llmMarkdown LLM Markdown 后处理配置
 * @param threadPools 线程池隔离配置
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
        OcrProperties ocr,
        PaddleOcrProperties paddleOcr,
        OcrHealthProperties ocrHealth,
        ExtractionProperties extraction,
        PdfRenderProperties pdfRender,
        WordConversionProperties wordConversion,
        LlmMarkdownProperties llmMarkdown,
        ThreadPoolsProperties threadPools
) {
    private static final String DEFAULT_PADDLE_OCR_ENDPOINT = "http://10.100.30.215:8080/ocr";

    public DocLensSpringProperties {
        callback = callback == null ? new CallbackProperties(3, 10) : callback;
        adapter = adapter == null ? new AdapterProperties("paddle_ocr") : adapter;
        ocr = ocr == null ? new OcrProperties(OcrRoutingMode.GLOBAL_LOAD_BALANCE, "least-inflight", 3, 30, 5,
                3, 2, false) : ocr;
        paddleOcr = paddleOcr == null ? defaultPaddleOcr() : paddleOcr;
        ocrHealth = ocrHealth == null ? new OcrHealthProperties(3, 2) : ocrHealth;
        extraction = extraction == null ? new ExtractionProperties(1) : extraction;
        pdfRender = pdfRender == null ? new PdfRenderProperties(36, "png") : pdfRender;
        wordConversion = wordConversion == null ? new WordConversionProperties("soffice", 60) : wordConversion;
        llmMarkdown = llmMarkdown == null ? new LlmMarkdownProperties("", "", "") : llmMarkdown;
        threadPools = threadPools == null ? ThreadPoolsProperties.defaults() : threadPools;
    }

    /**
     * 创建默认 PaddleOCR 配置。
     *
     * @return PaddleOCR 配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static PaddleOcrProperties defaultPaddleOcr() {
        return new PaddleOcrProperties(true, DEFAULT_PADDLE_OCR_ENDPOINT, 600, false, List.of(defaultPaddleNode()));
    }

    /**
     * 创建默认 PaddleOCR 启动节点。
     *
     * @return PaddleOCR 启动节点
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static PaddleOcrNodeProperties defaultPaddleNode() {
        return new PaddleOcrNodeProperties("paddle-215", "10.100.30.215", 8080, true, true, 100, 4);
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
     * OCR 路由与健康检查属性。
     *
     * @param defaultRoutingMode 默认路由模式
     * @param loadBalanceStrategy 默认负载均衡策略
     * @param requestRetryTimes 请求重试次数
     * @param healthCheckIntervalSeconds 健康检查间隔秒数
     * @param healthCheckTimeoutSeconds 健康检查超时秒数
     * @param healthFailureThreshold 健康失败摘除阈值
     * @param recoverySuccessThreshold 恢复成功阈值
     * @param specificNodeFallbackEnabled 指定节点是否允许回退
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public record OcrProperties(
            OcrRoutingMode defaultRoutingMode,
            String loadBalanceStrategy,
            int requestRetryTimes,
            int healthCheckIntervalSeconds,
            int healthCheckTimeoutSeconds,
            int healthFailureThreshold,
            int recoverySuccessThreshold,
            boolean specificNodeFallbackEnabled
    ) {
    }

    /**
     * PaddleOCR 属性。
     *
     * @param enabled 是否启用
     * @param endpoint 接口地址
     * @param timeoutSeconds 超时秒数
     * @param visualize 是否请求可视化
     * @param bootstrapNodes 启动初始化节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public record PaddleOcrProperties(
            boolean enabled,
            String endpoint,
            int timeoutSeconds,
            boolean visualize,
            List<PaddleOcrNodeProperties> bootstrapNodes
    ) {

        public PaddleOcrProperties {
            bootstrapNodes = bootstrapNodes == null ? List.of() : List.copyOf(bootstrapNodes);
        }
    }

    /**
     * PaddleOCR 启动节点属性。
     *
     * @param name 节点名称
     * @param host 节点主机
     * @param port 节点端口
     * @param enabled 是否启用
     * @param participateGlobal 是否参与全局负载均衡
     * @param weight 权重
     * @param maxConcurrency 最大并发
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public record PaddleOcrNodeProperties(
            String name,
            String host,
            int port,
            boolean enabled,
            boolean participateGlobal,
            int weight,
            int maxConcurrency
    ) {
    }

    /**
     * OCR 健康检查属性。
     *
     * @param healthFailureThreshold 健康检查失败摘除阈值
     * @param recoverySuccessThreshold 恢复成功阈值
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public record OcrHealthProperties(int healthFailureThreshold, int recoverySuccessThreshold) {
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

    /**
     * LLM Markdown 后处理属性。
     *
     * @param url OpenAI compatible endpoint
     * @param model 模型名称
     * @param apiKey API Key，可为空
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public record LlmMarkdownProperties(String url, String model, String apiKey) {
    }

    /**
     * DocLens 线程池隔离属性。
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

        /**
         * 创建默认线程池隔离配置。
         *
         * @return 默认线程池隔离配置
         * @author lvdaxianerplus
         * @date 2026-06-08
         */
        public static ThreadPoolsProperties defaults() {
            return new ThreadPoolsProperties(
                    new ThreadPoolProperties(1, 1, 1000, 60, "doclens-document-processing-"),
                    new ThreadPoolProperties(2, 4, 100, 60, "doclens-ocr-request-"),
                    new ThreadPoolProperties(1, 2, 100, 60, "doclens-ocr-health-"),
                    new ThreadPoolProperties(1, 2, 100, 60, "doclens-callback-")
            );
        }
    }

    /**
     * 单个业务线程池属性。
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
