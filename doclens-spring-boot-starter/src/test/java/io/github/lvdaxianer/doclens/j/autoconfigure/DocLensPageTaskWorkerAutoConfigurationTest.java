package io.github.lvdaxianer.doclens.j.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;

/**
 * 页任务 worker 自动配置测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-19
 */
class DocLensPageTaskWorkerAutoConfigurationTest {

    private static final int SINGLE_NODE_CONCURRENCY = 10;
    private static final int AGGREGATE_NODE_CONCURRENCY = 30;
    private static final int EXPLICIT_WORKER_CONCURRENCY = 12;
    private static final int DEFAULT_QUEUE_CAPACITY = 200;
    private static final int DEFAULT_RECOVERY_LIMIT = 32;
    private static final int DEFAULT_INTERVAL_MILLIS = 500;
    private static final int DEFAULT_PADDLE_TIMEOUT_SECONDS = 600;
    private static final int DEFAULT_PAGE_TASK_LOCK_SECONDS = 630;
    private static final int EXPLICIT_EXECUTOR_QUEUE_CAPACITY = 500;
    private static final int EXPLICIT_RECOVERY_LIMIT = 64;
    private static final int EXPLICIT_INTERVAL_MILLIS = 250;
    private static final int TEST_PADDLE_PORT = 8080;
    private static final int TEST_NODE_WEIGHT = 50;
    private static final String LOCAL_WORKER_ID = "local-worker";
    private static final String TEST_PADDLE_ENDPOINT = "http://127.0.0.1:8080/ocr";
    private static final String TEST_PADDLE_HOST = "127.0.0.1";

    /**
     * 默认页任务锁应覆盖 OCR 请求超时窗口。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void effectiveSettingsKeepPageTaskLockLongerThanOcrTimeout() {
        DocLensSpringProperties properties = defaultPropertiesWithPaddleTimeout(DEFAULT_PADDLE_TIMEOUT_SECONDS);

        DocLensPageTaskWorkerAutoConfiguration.PageTaskWorkerRuntimeSettings settings =
                new DocLensPageTaskWorkerAutoConfiguration().pageTaskWorkerRuntimeSettings(properties);

        assertThat(settings.lockSeconds()).isGreaterThanOrEqualTo(DEFAULT_PAGE_TASK_LOCK_SECONDS);
    }

    /**
     * 默认页任务并发应跟随启用节点的最大并发。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void effectiveSettingsUseBootstrapNodeConcurrencyWhenNotExplicitlyConfigured() {
        DocLensSpringProperties properties = defaultPropertiesWithPaddleTimeout(DEFAULT_PADDLE_TIMEOUT_SECONDS);

        DocLensPageTaskWorkerAutoConfiguration.PageTaskWorkerRuntimeSettings settings =
                new DocLensPageTaskWorkerAutoConfiguration().pageTaskWorkerRuntimeSettings(properties);

        assertThat(settings.batchSize()).isEqualTo(SINGLE_NODE_CONCURRENCY);
        assertThat(settings.poolSize()).isEqualTo(SINGLE_NODE_CONCURRENCY);
    }

    /**
     * 默认页任务并发应跟随参与全局路由的 OCR 节点总并发。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void effectiveSettingsUseAggregateParticipatingNodeConcurrencyWhenNotExplicitlyConfigured() {
        DocLensSpringProperties properties = propertiesWithPaddleNodes(
                paddleNode("paddle-a", true, true, SINGLE_NODE_CONCURRENCY),
                paddleNode("paddle-b", true, true, SINGLE_NODE_CONCURRENCY),
                paddleNode("paddle-c", true, true, SINGLE_NODE_CONCURRENCY),
                paddleNode("paddle-disabled", false, true, SINGLE_NODE_CONCURRENCY),
                paddleNode("paddle-local", true, false, SINGLE_NODE_CONCURRENCY));

        DocLensPageTaskWorkerAutoConfiguration.PageTaskWorkerRuntimeSettings settings =
                new DocLensPageTaskWorkerAutoConfiguration().pageTaskWorkerRuntimeSettings(properties);

        assertThat(settings.batchSize()).isEqualTo(AGGREGATE_NODE_CONCURRENCY);
        assertThat(settings.poolSize()).isEqualTo(AGGREGATE_NODE_CONCURRENCY);
        assertThat(DocLensPageTaskWorkerAutoConfiguration.derivedNodeConcurrency(properties))
                .isEqualTo(AGGREGATE_NODE_CONCURRENCY);
    }

    /**
     * 显式配置页任务并发时应优先使用运维配置。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void effectiveSettingsRespectExplicitPageTaskWorkerConcurrency() {
        DocLensSpringProperties.PageTaskWorkerProperties worker =
                new DocLensSpringProperties.PageTaskWorkerProperties(EXPLICIT_WORKER_CONCURRENCY, 0,
                        EXPLICIT_WORKER_CONCURRENCY, DEFAULT_QUEUE_CAPACITY, DEFAULT_RECOVERY_LIMIT,
                        DEFAULT_INTERVAL_MILLIS);
        DocLensSpringProperties properties = propertiesWithWorker(worker,
                paddleNode("paddle-a", true, true, SINGLE_NODE_CONCURRENCY),
                paddleNode("paddle-b", true, true, SINGLE_NODE_CONCURRENCY),
                paddleNode("paddle-c", true, true, SINGLE_NODE_CONCURRENCY));

        DocLensPageTaskWorkerAutoConfiguration.PageTaskWorkerRuntimeSettings settings =
                new DocLensPageTaskWorkerAutoConfiguration().pageTaskWorkerRuntimeSettings(properties);

        assertThat(settings.batchSize()).isEqualTo(EXPLICIT_WORKER_CONCURRENCY);
        assertThat(settings.poolSize()).isEqualTo(EXPLICIT_WORKER_CONCURRENCY);
    }

    /**
     * 页任务执行线程池应使用生效后的 pool size。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void pageTaskExecutorUsesEffectivePoolSize() {
        DocLensPageTaskWorkerAutoConfiguration configuration = new DocLensPageTaskWorkerAutoConfiguration();
        DocLensPageTaskWorkerAutoConfiguration.PageTaskWorkerRuntimeSettings settings =
                new DocLensPageTaskWorkerAutoConfiguration.PageTaskWorkerRuntimeSettings(SINGLE_NODE_CONCURRENCY,
                        DEFAULT_PAGE_TASK_LOCK_SECONDS, SINGLE_NODE_CONCURRENCY, EXPLICIT_EXECUTOR_QUEUE_CAPACITY,
                        EXPLICIT_RECOVERY_LIMIT, EXPLICIT_INTERVAL_MILLIS);

        ThreadPoolExecutor executor = (ThreadPoolExecutor) configuration.doclensPageTaskExecutor(settings);

        try {
            assertThat(executor.getCorePoolSize()).isEqualTo(SINGLE_NODE_CONCURRENCY);
            assertThat(executor.getMaximumPoolSize()).isEqualTo(SINGLE_NODE_CONCURRENCY);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 创建指定 PaddleOCR 超时的默认配置。
     *
     * @param timeoutSeconds PaddleOCR 请求超时秒数
     * @return DocLens Spring 配置
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private DocLensSpringProperties defaultPropertiesWithPaddleTimeout(int timeoutSeconds) {
        return new DocLensSpringProperties(null, true, LOCAL_WORKER_ID, null, null, null, null,
                new DocLensSpringProperties.PaddleOcrProperties(true, TEST_PADDLE_ENDPOINT,
                        timeoutSeconds, false, List.of(DocLensSpringProperties.defaultPaddleNode())),
                null, null, null, null, null, null, null, null);
    }

    /**
     * 创建指定 OCR 节点集合的配置。
     *
     * @param nodes OCR 节点集合
     * @return DocLens Spring 配置
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private DocLensSpringProperties propertiesWithPaddleNodes(DocLensSpringProperties.PaddleOcrNodeProperties... nodes) {
        return propertiesWithWorker(null, nodes);
    }

    /**
     * 创建指定页任务 worker 与 OCR 节点集合的配置。
     *
     * @param worker 页任务 worker 配置
     * @param nodes OCR 节点集合
     * @return DocLens Spring 配置
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private DocLensSpringProperties propertiesWithWorker(
            DocLensSpringProperties.PageTaskWorkerProperties worker,
            DocLensSpringProperties.PaddleOcrNodeProperties... nodes
    ) {
        return new DocLensSpringProperties(null, true, LOCAL_WORKER_ID, null, null, null, null,
                new DocLensSpringProperties.PaddleOcrProperties(true, TEST_PADDLE_ENDPOINT,
                        DEFAULT_PADDLE_TIMEOUT_SECONDS, false, List.of(nodes)),
                null, null, null, null, null, worker, null, null);
    }

    /**
     * 创建测试 OCR 节点配置。
     *
     * @param name 节点名称
     * @param enabled 是否启用
     * @param participateGlobal 是否参与全局路由
     * @param maxConcurrency 最大并发
     * @return OCR 节点配置
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private DocLensSpringProperties.PaddleOcrNodeProperties paddleNode(
            String name,
            boolean enabled,
            boolean participateGlobal,
            int maxConcurrency
    ) {
        return new DocLensSpringProperties.PaddleOcrNodeProperties(name, TEST_PADDLE_HOST, TEST_PADDLE_PORT, enabled,
                participateGlobal, TEST_NODE_WEIGHT, maxConcurrency);
    }
}
