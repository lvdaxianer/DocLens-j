package io.github.lvdaxianer.doclens.j.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;

/**
 * 回调投递线程池自动配置测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-16
 */
class DocLensOcrThreadPoolAutoConfigurationCallbackTest {

    private static final int EXPECTED_CALLBACK_CORE_SIZE = 3;
    private static final int DASHBOARD_TOTAL_CONCURRENCY = 24;
    private static final int DASHBOARD_DEFAULT_NODE_CONCURRENCY = 10;
    private static final int DASHBOARD_SMALL_NODE_CONCURRENCY = 4;
    private static final int EXPLICIT_OCR_REQUEST_CORE_SIZE = 6;
    private static final int EXPLICIT_OCR_REQUEST_MAX_SIZE = 8;
    private static final int EXPLICIT_OCR_REQUEST_QUEUE_CAPACITY = 128;
    private static final int EXPLICIT_OCR_REQUEST_KEEP_ALIVE_SECONDS = 30;
    private static final String EXPLICIT_OCR_REQUEST_THREAD_PREFIX = "custom-ocr-request-";

    /**
     * 回调投递线程池应使用 3 个核心线程并支持延迟调度。
     *
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @Test
    void callbackExecutorUsesThreeCoreScheduledPool() {
        ExecutorService executor = new DocLensOcrThreadPoolAutoConfiguration()
                .doclensCallbackExecutor(defaultProperties());

        try {
            assertThat(executor).isInstanceOf(ScheduledThreadPoolExecutor.class);
            assertThat(((ScheduledThreadPoolExecutor) executor).getCorePoolSize())
                    .isEqualTo(EXPECTED_CALLBACK_CORE_SIZE);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 默认 OCR 请求线程池应按启用节点最大并发扩容。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void ocrRequestExecutorDefaultsToBootstrapNodeConcurrency() {
        ExecutorService executor = new DocLensOcrThreadPoolAutoConfiguration()
                .doclensOcrRequestExecutor(defaultProperties());

        try {
            ThreadPoolExecutor threadPool = (ThreadPoolExecutor) executor;
            assertThat(threadPool.getCorePoolSize()).isEqualTo(DASHBOARD_DEFAULT_NODE_CONCURRENCY);
            assertThat(threadPool.getMaximumPoolSize()).isEqualTo(DASHBOARD_DEFAULT_NODE_CONCURRENCY);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 默认 OCR 请求线程池应优先使用页面健康节点总并发。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void ocrRequestExecutorDefaultsToDashboardNodeConcurrency() {
        OcrNodeRepository repository = OcrRuntimeConcurrencyResolverTest.repository(
                OcrRuntimeConcurrencyResolverTest.upNode("dashboard-a", DASHBOARD_DEFAULT_NODE_CONCURRENCY),
                OcrRuntimeConcurrencyResolverTest.upNode("dashboard-b", DASHBOARD_SMALL_NODE_CONCURRENCY),
                OcrRuntimeConcurrencyResolverTest.upNode("dashboard-c", DASHBOARD_DEFAULT_NODE_CONCURRENCY));

        ExecutorService executor = new DocLensOcrThreadPoolAutoConfiguration()
                .doclensOcrRequestExecutor(defaultProperties(), repository);

        try {
            ThreadPoolExecutor threadPool = (ThreadPoolExecutor) executor;
            assertThat(threadPool.getCorePoolSize()).isEqualTo(DASHBOARD_TOTAL_CONCURRENCY);
            assertThat(threadPool.getMaximumPoolSize()).isEqualTo(DASHBOARD_TOTAL_CONCURRENCY);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 显式配置 OCR 请求线程池时应优先使用运维配置。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void ocrRequestExecutorRespectsExplicitThreadPoolConfiguration() {
        OcrNodeRepository repository = OcrRuntimeConcurrencyResolverTest.repository(
                OcrRuntimeConcurrencyResolverTest.upNode("dashboard-a", DASHBOARD_DEFAULT_NODE_CONCURRENCY),
                OcrRuntimeConcurrencyResolverTest.upNode("dashboard-b", DASHBOARD_SMALL_NODE_CONCURRENCY),
                OcrRuntimeConcurrencyResolverTest.upNode("dashboard-c", DASHBOARD_DEFAULT_NODE_CONCURRENCY));

        ExecutorService executor = new DocLensOcrThreadPoolAutoConfiguration()
                .doclensOcrRequestExecutor(propertiesWithExplicitOcrRequestThreadPool(), repository);

        try {
            ThreadPoolExecutor threadPool = (ThreadPoolExecutor) executor;
            assertThat(threadPool.getCorePoolSize()).isEqualTo(EXPLICIT_OCR_REQUEST_CORE_SIZE);
            assertThat(threadPool.getMaximumPoolSize()).isEqualTo(EXPLICIT_OCR_REQUEST_MAX_SIZE);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 创建默认 DocLens Spring 配置。
     *
     * @return 默认 DocLens Spring 配置
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private DocLensSpringProperties defaultProperties() {
        return new DocLensSpringProperties(null, true, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    /**
     * 创建显式配置 OCR 请求线程池的 DocLens Spring 配置。
     *
     * @return 显式配置 OCR 请求线程池的 DocLens Spring 配置
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private DocLensSpringProperties propertiesWithExplicitOcrRequestThreadPool() {
        DocLensSpringProperties.ThreadPoolProperties defaultPool =
                new DocLensSpringProperties.ThreadPoolProperties(EXPLICIT_OCR_REQUEST_CORE_SIZE,
                        EXPLICIT_OCR_REQUEST_CORE_SIZE, EXPLICIT_OCR_REQUEST_QUEUE_CAPACITY,
                        EXPLICIT_OCR_REQUEST_KEEP_ALIVE_SECONDS, EXPLICIT_OCR_REQUEST_THREAD_PREFIX);
        DocLensSpringProperties.ThreadPoolProperties explicitOcrRequestPool =
                new DocLensSpringProperties.ThreadPoolProperties(EXPLICIT_OCR_REQUEST_CORE_SIZE,
                        EXPLICIT_OCR_REQUEST_MAX_SIZE, EXPLICIT_OCR_REQUEST_QUEUE_CAPACITY,
                        EXPLICIT_OCR_REQUEST_KEEP_ALIVE_SECONDS, EXPLICIT_OCR_REQUEST_THREAD_PREFIX);
        DocLensSpringProperties.ThreadPoolsProperties threadPools =
                new DocLensSpringProperties.ThreadPoolsProperties(defaultPool, explicitOcrRequestPool,
                        defaultPool, defaultPool);
        return new DocLensSpringProperties(null, true, null, null, null, null, null, null, null, null,
                null, null, null, null, null, threadPools, null);
    }
}
