package io.github.lvdaxianer.doclens.j.autoconfigure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingServiceProperties;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrHealthGovernance;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * 绑定到 Spring 的 DocLens 配置属性。
 *
 * @param storageRoot 存储根目录
 * @param autoProcessOnUpload 自动处理标志
 * @param workerId Worker 标识
 * @param clients 接入方配置
 * @param gatewayAuth 可信网关认证配置
 * @param callback 回调配置
 * @param adapter 适配器配置
 * @param ocr OCR 路由配置
 * @param paddleOcr PaddleOCR 配置
 * @param ocrHealth OCR 健康检查配置
 * @param extraction 提取配置
 * @param pdfRender PDF 渲染配置
 * @param wordConversion Word 转 PDF 配置
 * @param llmMarkdown LLM Markdown 后处理配置
 * @param pageTaskWorker 页任务 worker 配置
 * @param threadPools 线程池隔离配置
 * @param traffic 调用方流量治理配置
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@ConfigurationProperties(prefix = "doclens")
public record DocLensSpringProperties(
        String storageRoot,
        boolean autoProcessOnUpload,
        String workerId,
        ClientsProperties clients,
        GatewayAuthProperties gatewayAuth,
        CallbackProperties callback,
        AdapterProperties adapter,
        OcrProperties ocr,
        PaddleOcrProperties paddleOcr,
        OcrHealthProperties ocrHealth,
        ExtractionProperties extraction,
        PdfRenderProperties pdfRender,
        WordConversionProperties wordConversion,
        LlmMarkdownProperties llmMarkdown,
        PageTaskWorkerProperties pageTaskWorker,
        ThreadPoolsProperties threadPools,
        TrafficProperties traffic
) {
    private static final String DEFAULT_PADDLE_OCR_ENDPOINT = "http://10.100.30.215:8080/ocr";
    private static final int DEFAULT_HEALTH_CHECK_TIMEOUT_SECONDS = 5;
    private static final int DEFAULT_EXTRACTION_OCR_CONCURRENCY = 4;
    private static final int DEFAULT_DOCUMENT_PROCESSING_CONCURRENCY = 6;
    private static final int DEFAULT_CALLBACK_DELIVERY_CONCURRENCY = 3;
    private static final int DEFAULT_OCR_REQUEST_CORE_SIZE = 2;
    private static final int DEFAULT_OCR_REQUEST_MAX_SIZE = 4;
    private static final int DEFAULT_THREAD_POOL_QUEUE_CAPACITY = 100;
    private static final int DEFAULT_THREAD_POOL_KEEP_ALIVE_SECONDS = 60;
    private static final String DEFAULT_OCR_REQUEST_THREAD_PREFIX = "doclens-ocr-request-";
    private static final int DEFAULT_NODE_WEIGHT = 50;
    private static final int DEFAULT_NODE_MAX_CONCURRENCY = 10;
    private static final int DEFAULT_PAGE_TASK_WORKER_INTERVAL_MILLIS = 500;
    private static final int DEFAULT_PAGE_TASK_WORKER_BATCH_SIZE = 0;
    private static final int DEFAULT_PAGE_TASK_LOCK_SECONDS = 0;
    private static final int DEFAULT_PAGE_TASK_WORKER_POOL_SIZE = 0;
    private static final int DEFAULT_PAGE_TASK_WORKER_QUEUE_CAPACITY = 200;
    private static final int DEFAULT_PAGE_TASK_RECOVERY_LIMIT = 32;
    @ConstructorBinding
    public DocLensSpringProperties {
        clients = clients == null ? new ClientsProperties(List.of()) : clients;
        gatewayAuth = gatewayAuth == null ? GatewayAuthProperties.defaults() : gatewayAuth;
        callback = callback == null ? new CallbackProperties(3, 10) : callback;
        adapter = adapter == null ? new AdapterProperties("paddle_ocr") : adapter;
        ocr = ocr == null ? defaultOcrProperties() : ocr;
        paddleOcr = paddleOcr == null ? defaultPaddleOcr() : paddleOcr;
        ocrHealth = ocrHealth == null ? defaultOcrHealth() : ocrHealth;
        extraction = extraction == null ? new ExtractionProperties(DEFAULT_EXTRACTION_OCR_CONCURRENCY) : extraction;
        pdfRender = pdfRender == null ? new PdfRenderProperties(36, "png") : pdfRender;
        wordConversion = wordConversion == null ? new WordConversionProperties("soffice", 60) : wordConversion;
        llmMarkdown = llmMarkdown == null ? new LlmMarkdownProperties("", "", "") : llmMarkdown;
        pageTaskWorker = pageTaskWorker == null ? PageTaskWorkerProperties.defaults() : pageTaskWorker;
        threadPools = threadPools == null ? ThreadPoolsProperties.defaults() : threadPools;
        traffic = traffic == null ? TrafficProperties.defaults() : traffic;
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
     * 创建默认 OCR 路由配置。
     *
     * @return 默认 OCR 路由配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static OcrProperties defaultOcrProperties() {
        return new OcrProperties(OcrRoutingMode.GLOBAL_LOAD_BALANCE, OcrRoutingServiceProperties.DEFAULT_LOAD_BALANCE_STRATEGY,
                OcrRoutingServiceProperties.DEFAULT_IDLE_FACTOR, OcrRoutingServiceProperties.DEFAULT_WEIGHT_FACTOR,
                OcrRoutingServiceProperties.DEFAULT_TOP_BUCKET_THRESHOLD,
                OcrRoutingServiceProperties.DEFAULT_REQUEST_RETRY_TIMES,
                OcrHealthGovernance.DEFAULT_PROBE_INTERVAL_SECONDS, DEFAULT_HEALTH_CHECK_TIMEOUT_SECONDS,
                OcrHealthGovernance.DEFAULT_FAILURE_THRESHOLD, OcrHealthGovernance.DEFAULT_CIRCUIT_OPEN_SECONDS,
                OcrHealthGovernance.DEFAULT_RECOVERY_SUCCESS_THRESHOLD,
                OcrHealthGovernance.DEFAULT_MANUAL_RECOVERY_ATTEMPTS, false);
    }

    /**
     * 创建默认 OCR 健康检查配置。
     *
     * @return 默认 OCR 健康检查配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static OcrHealthProperties defaultOcrHealth() {
        return new OcrHealthProperties(OcrHealthGovernance.DEFAULT_FAILURE_THRESHOLD,
                OcrHealthGovernance.DEFAULT_RECOVERY_SUCCESS_THRESHOLD);
    }

    /**
     * 创建默认 PaddleOCR 启动节点。
     *
     * @return PaddleOCR 启动节点
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    static PaddleOcrNodeProperties defaultPaddleNode() {
        return new PaddleOcrNodeProperties("paddle-215", "10.100.30.215", 8080, true, true, DEFAULT_NODE_WEIGHT,
                DEFAULT_NODE_MAX_CONCURRENCY);
    }

    /**
     * 接入方配置属性。
     *
     * @param credentials 接入方凭证集合
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public record ClientsProperties(List<CallerCredentialProperties> credentials) {

        /**
         * 创建接入方配置属性。
         *
         * @author lvdaxianerplus
         * @date 2026-06-17
         */
        public ClientsProperties {
            credentials = credentials == null ? List.of() : List.copyOf(credentials);
        }
    }

    /**
     * 单个接入方凭证属性。
     *
     * @param clientId 接入方标识
     * @param sourceApp 来源应用
     * @param tenantKey 租户或业务分区键
     * @param apiKey API Key
     * @param bearerToken Bearer Token
     * @param rateLimits 调用方接口组限流覆盖
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public record CallerCredentialProperties(
            String clientId,
            String sourceApp,
            String tenantKey,
            String apiKey,
            String bearerToken,
            Map<String, RateLimitProperties> rateLimits
    ) {
        /**
         * 按接口组名称读取调用方限流覆盖。
         *
         * @param groupName 接口组名称
         * @return 调用方限流覆盖
         * @author lvdaxianerplus
         * @date 2026-06-17
         */
        public Optional<RateLimitProperties> rateLimit(String groupName) {
            return findRateLimit(rateLimits, groupName);
        }

        public CallerCredentialProperties {
            rateLimits = rateLimits == null ? Map.of() : Map.copyOf(rateLimits);
        }
    }

    /**
     * DocLens 调用方流量治理属性。
     *
     * @param enabled 是否启用调用方限流
     * @param anonymousEnabled 是否允许匿名调用方
     * @param defaultLimits 默认接口组限流
     * @param globalProtection 全局保护配置
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public record TrafficProperties(
            boolean enabled,
            boolean anonymousEnabled,
            Map<String, RateLimitProperties> defaultLimits,
            GlobalProtectionProperties globalProtection
    ) {

        /**
         * 创建默认调用方流量治理属性。
         *
         * @return 默认调用方流量治理属性
         * @author lvdaxianerplus
         * @date 2026-06-17
         */
        public static TrafficProperties defaults() {
            return new TrafficProperties(true, false, Map.of(), GlobalProtectionProperties.defaults());
        }

        /**
         * 按接口组名称读取默认限流配置。
         *
         * @param groupName 接口组名称
         * @return 默认限流配置
         * @author lvdaxianerplus
         * @date 2026-06-17
         */
        public Optional<RateLimitProperties> defaultLimit(String groupName) {
            return findRateLimit(defaultLimits, groupName);
        }

        @ConstructorBinding
        public TrafficProperties {
            defaultLimits = defaultLimits == null ? Map.of() : Map.copyOf(defaultLimits);
            globalProtection = globalProtection == null ? GlobalProtectionProperties.defaults() : globalProtection;
        }
    }

    /**
     * 单个接口组限流属性。
     *
     * @param qps 每秒令牌补充数量
     * @param burst 最大突发令牌数
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public record RateLimitProperties(double qps, int burst) {
    }

    /**
     * 全局并发保护属性。
     *
     * @param enabled 是否启用全局保护
     * @param maxInFlight 最大进行中请求数
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public record GlobalProtectionProperties(boolean enabled, int maxInFlight) {

        /**
         * 创建默认全局保护属性。
         *
         * @return 默认全局保护属性
         * @author lvdaxianerplus
         * @date 2026-06-17
         */
        public static GlobalProtectionProperties defaults() {
            return new GlobalProtectionProperties(true, 100);
        }
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
     * @param idleFactor 空闲容量评分权重
     * @param weightFactor 节点权重评分权重
     * @param topBucketThreshold Top Bucket 分桶阈值
     * @param requestRetryTimes 请求重试次数
     * @param probeIntervalSeconds 健康检查间隔秒数
     * @param healthCheckTimeoutSeconds 健康检查超时秒数
     * @param failureThreshold 健康失败摘除阈值
     * @param circuitOpenSeconds 熔断打开持续秒数
     * @param recoverySuccessThreshold 恢复成功阈值
     * @param manualRecoveryAttempts 手动恢复尝试次数
     * @param specificNodeFallbackEnabled 指定节点是否允许回退
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public record OcrProperties(
            OcrRoutingMode defaultRoutingMode,
            String loadBalanceStrategy,
            double idleFactor,
            double weightFactor,
            double topBucketThreshold,
            int requestRetryTimes,
            int probeIntervalSeconds,
            int healthCheckTimeoutSeconds,
            int failureThreshold,
            int circuitOpenSeconds,
            int recoverySuccessThreshold,
            int manualRecoveryAttempts,
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
     * 页任务 worker 运行时配置。
     *
     * @param batchSize 每轮抢占页任务数量，0 表示跟随节点并发
     * @param lockSeconds 页任务锁秒数，0 表示根据 OCR 请求超时派生
     * @param poolSize 页任务执行线程数，0 表示跟随节点并发
     * @param queueCapacity 页任务执行队列容量
     * @param recoveryLimit 每轮恢复过期页任务数量
     * @param intervalMillis 页任务扫描间隔毫秒
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    public record PageTaskWorkerProperties(
            int batchSize,
            int lockSeconds,
            int poolSize,
            int queueCapacity,
            int recoveryLimit,
            int intervalMillis
    ) {

        /**
         * 创建默认页任务 worker 配置。
         *
         * @return 默认页任务 worker 配置
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        public static PageTaskWorkerProperties defaults() {
            return new PageTaskWorkerProperties(DEFAULT_PAGE_TASK_WORKER_BATCH_SIZE,
                    DEFAULT_PAGE_TASK_LOCK_SECONDS, DEFAULT_PAGE_TASK_WORKER_POOL_SIZE,
                    DEFAULT_PAGE_TASK_WORKER_QUEUE_CAPACITY, DEFAULT_PAGE_TASK_RECOVERY_LIMIT,
                    DEFAULT_PAGE_TASK_WORKER_INTERVAL_MILLIS);
        }
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
                    new ThreadPoolProperties(DEFAULT_DOCUMENT_PROCESSING_CONCURRENCY,
                            DEFAULT_DOCUMENT_PROCESSING_CONCURRENCY, 1000,
                            DEFAULT_THREAD_POOL_KEEP_ALIVE_SECONDS, "doclens-document-processing-"),
                    new ThreadPoolProperties(DEFAULT_OCR_REQUEST_CORE_SIZE, DEFAULT_OCR_REQUEST_MAX_SIZE,
                            DEFAULT_THREAD_POOL_QUEUE_CAPACITY, DEFAULT_THREAD_POOL_KEEP_ALIVE_SECONDS,
                            DEFAULT_OCR_REQUEST_THREAD_PREFIX),
                    new ThreadPoolProperties(1, DEFAULT_OCR_REQUEST_CORE_SIZE, DEFAULT_THREAD_POOL_QUEUE_CAPACITY,
                            DEFAULT_THREAD_POOL_KEEP_ALIVE_SECONDS, "doclens-ocr-health-"),
                    new ThreadPoolProperties(DEFAULT_CALLBACK_DELIVERY_CONCURRENCY,
                            DEFAULT_CALLBACK_DELIVERY_CONCURRENCY, DEFAULT_THREAD_POOL_QUEUE_CAPACITY,
                            DEFAULT_THREAD_POOL_KEEP_ALIVE_SECONDS, "doclens-callback-")
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
        /**
         * 判断是否为默认 OCR 请求线程池配置。
         *
         * @return 是否默认 OCR 请求线程池配置
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        public boolean isDefaultOcrRequestThreadPool() {
            return coreSize == DEFAULT_OCR_REQUEST_CORE_SIZE && maxSize == DEFAULT_OCR_REQUEST_MAX_SIZE
                    && queueCapacity == DEFAULT_THREAD_POOL_QUEUE_CAPACITY
                    && keepAliveSeconds == DEFAULT_THREAD_POOL_KEEP_ALIVE_SECONDS
                    && DEFAULT_OCR_REQUEST_THREAD_PREFIX.equals(threadNamePrefix);
        }

        /**
         * 创建指定核心和最大线程数的线程池配置。
         *
         * @param size 线程池大小
         * @return 新线程池配置
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        public ThreadPoolProperties withSize(int size) {
            int safeSize = Math.max(1, size);
            return new ThreadPoolProperties(safeSize, safeSize, queueCapacity, keepAliveSeconds, threadNamePrefix);
        }
    }

    /**
     * 按规范化后的接口组名称查找限流配置。
     *
     * @param rateLimits 限流配置集合
     * @param groupName 接口组名称
     * @return 匹配到的限流配置
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private static Optional<RateLimitProperties> findRateLimit(Map<String, RateLimitProperties> rateLimits,
                                                               String groupName) {
        RateLimitProperties directMatch = rateLimits.get(groupName);
        if (directMatch != null) {
            return Optional.of(directMatch);
        } else {
            String normalizedGroupName = normalizeTrafficGroupName(groupName);
            for (Map.Entry<String, RateLimitProperties> entry : rateLimits.entrySet()) {
                if (normalizeTrafficGroupName(entry.getKey()).equals(normalizedGroupName)) {
                    return Optional.of(entry.getValue());
                }
            }
            return Optional.empty();
        }
    }

    /**
     * 统一接口组名称的比较口径，兼容 Spring 配置绑定对分隔符的规范化。
     *
     * @param groupName 原始接口组名称
     * @return 规范化后的接口组名称
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private static String normalizeTrafficGroupName(String groupName) {
        return groupName == null ? "" : groupName.replace("-", "").replace("_", "").replace(".", "")
                .toLowerCase(Locale.ROOT);
    }
}
