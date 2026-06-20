package io.github.lvdaxianer.doclens.j.query.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

/**
 * OCR Dashboard 资源指标提供器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class OcrDashboardMetricsProviderTest extends OcrDashboardMetricsProviderTestSupport {

    /** OCR 请求核心线程数。 */
    private static final int OCR_REQUEST_CORE_POOL_SIZE = 3;
    /** OCR 请求最大线程数。 */
    private static final int OCR_REQUEST_MAXIMUM_POOL_SIZE = 30;
    /** OCR 请求队列容量。 */
    private static final int OCR_REQUEST_QUEUE_CAPACITY = 50;
    /** 页任务 worker 线程数。 */
    private static final int PAGE_TASK_WORKER_POOL_SIZE = 30;
    /** 页任务 worker 队列容量。 */
    private static final int PAGE_TASK_WORKER_QUEUE_CAPACITY = 200;
    /** 页任务 worker 抢占批量。 */
    private static final int PAGE_TASK_WORKER_BATCH_SIZE = 18;
    /** 页任务 worker 锁秒数。 */
    private static final int PAGE_TASK_WORKER_LOCK_SECONDS = 630;
    /** 页任务 worker 恢复上限。 */
    private static final int PAGE_TASK_WORKER_RECOVERY_LIMIT = 32;
    /** 页任务 worker 轮询间隔。 */
    private static final int PAGE_TASK_WORKER_INTERVAL_MILLIS = 500;

    /*
     * 该类只保留 OCR 资源面板指标。
     * 批次命中和最终命中节点分配场景，
     * 已拆到 OcrDashboardHitNodesMetricsProviderTest。
     */

    /**
     * OCR 资源指标应基于调用记录计算今日处理、平均耗时和 P95。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void ocrResourcesIncludesLatencyMetricsFromCalls() {
        InMemoryOcrNodeRepository nodeRepository = new InMemoryOcrNodeRepository(List.of(
                offlineNode(FINANCE_NODE_ID, FINANCE_NODE_NAME),
                offlineNode(INVOICE_NODE_ID, INVOICE_NODE_NAME)
        ));
        InMemoryOcrNodeCallRepository callRepository = new InMemoryOcrNodeCallRepository(List.of(
                successfulCall(callSeed("call-1", "batch-1", "doc-1", 1), FINANCE_NODE_ID, 100),
                successfulCall(callSeed("call-2", "batch-1", "doc-1", 2), FINANCE_NODE_ID, 200),
                successfulCall(callSeed("call-3", "batch-2", "doc-2", 1), INVOICE_NODE_ID, 600)
        ));
        OcrDashboardMetricsProvider provider = provider(nodeRepository, callRepository);

        Map<String, Object> resources = provider.ocrResources();

        assertThat(resources).containsEntry("healthy_node_count", 2L);
        assertResourceNodes(resources);
    }

    /**
     * OCR 资源指标应展示请求线程池容量和页任务 worker 生效容量。
     *
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Test
    void ocrResourcesIncludesThreadPoolCapacityAndPageTaskWorkerRuntime() {
        InMemoryOcrNodeRepository nodeRepository = new InMemoryOcrNodeRepository(List.of(
                offlineNode(FINANCE_NODE_ID, FINANCE_NODE_NAME)
        ));
        InMemoryOcrNodeCallRepository callRepository = new InMemoryOcrNodeCallRepository(List.of());
        ThreadPoolExecutor ocrRequestExecutor = testExecutor(OCR_REQUEST_CORE_POOL_SIZE,
                OCR_REQUEST_MAXIMUM_POOL_SIZE, OCR_REQUEST_QUEUE_CAPACITY);
        ThreadPoolExecutor pageTaskExecutor = testExecutor(PAGE_TASK_WORKER_POOL_SIZE,
                PAGE_TASK_WORKER_POOL_SIZE, PAGE_TASK_WORKER_QUEUE_CAPACITY);
        try {
            OcrDashboardMetricsProvider provider = providerWithThreadPools(nodeRepository, callRepository,
                    dashboardThreadPools(ocrRequestExecutor, pageTaskExecutor));

            Map<String, Object> resources = provider.ocrResources();

            assertThreadPoolCapacity(resources);
        } finally {
            shutdownExecutors(ocrRequestExecutor, pageTaskExecutor);
        }
    }

    /**
     * 创建 Dashboard 线程池集合。
     *
     * @param ocrRequestExecutor OCR 请求线程池
     * @param pageTaskExecutor 页任务 OCR 执行线程池
     * @return Dashboard 线程池集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private DashboardThreadPools dashboardThreadPools(
            ThreadPoolExecutor ocrRequestExecutor,
            ThreadPoolExecutor pageTaskExecutor
    ) {
        DashboardCoreThreadPools coreThreadPools =
                new DashboardCoreThreadPools(null, ocrRequestExecutor, null, null, null);
        return new DashboardThreadPools(coreThreadPools, Optional.of(pageTaskExecutor),
                Optional.of(pageTaskWorkerSettings()));
    }

    /**
     * 断言线程池容量指标。
     *
     * @param resources 资源指标响应
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private void assertThreadPoolCapacity(Map<String, Object> resources) {
        assertThat(resources.get("thread_pools")).asInstanceOf(InstanceOfAssertFactories.MAP)
                .extractingByKey("ocr_request")
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("core_pool_size", OCR_REQUEST_CORE_POOL_SIZE)
                .containsEntry("maximum_pool_size", OCR_REQUEST_MAXIMUM_POOL_SIZE)
                .containsKey("largest_pool_size");
        assertThat(resources.get("thread_pools")).asInstanceOf(InstanceOfAssertFactories.MAP)
                .extractingByKey("page_task_worker")
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("pool_size", PAGE_TASK_WORKER_POOL_SIZE)
                .containsEntry("runtime_pool_size", 0)
                .containsEntry("core_pool_size", PAGE_TASK_WORKER_POOL_SIZE)
                .containsEntry("maximum_pool_size", PAGE_TASK_WORKER_POOL_SIZE)
                .containsEntry("batch_size", PAGE_TASK_WORKER_BATCH_SIZE)
                .containsEntry("lock_seconds", PAGE_TASK_WORKER_LOCK_SECONDS)
                .containsEntry("queue_capacity", PAGE_TASK_WORKER_QUEUE_CAPACITY)
                .containsEntry("recovery_limit", PAGE_TASK_WORKER_RECOVERY_LIMIT)
                .containsEntry("interval_millis", PAGE_TASK_WORKER_INTERVAL_MILLIS);
    }

    /**
     * 创建页任务 worker 生效设置。
     *
     * @return 页任务 worker 生效设置
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private DashboardPageTaskWorkerSettings pageTaskWorkerSettings() {
        return new DashboardPageTaskWorkerSettings(pageTaskExecutionSettings(),
                pageTaskRecoverySettings());
    }

    /**
     * 创建页任务 worker 执行设置。
     *
     * @return 页任务 worker 执行设置
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private DashboardPageTaskWorkerSettings.Execution pageTaskExecutionSettings() {
        return new DashboardPageTaskWorkerSettings.Execution(PAGE_TASK_WORKER_BATCH_SIZE,
                PAGE_TASK_WORKER_LOCK_SECONDS, PAGE_TASK_WORKER_POOL_SIZE, PAGE_TASK_WORKER_QUEUE_CAPACITY);
    }

    /**
     * 创建页任务 worker 恢复设置。
     *
     * @return 页任务 worker 恢复设置
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private DashboardPageTaskWorkerSettings.Recovery pageTaskRecoverySettings() {
        return new DashboardPageTaskWorkerSettings.Recovery(
                PAGE_TASK_WORKER_RECOVERY_LIMIT, PAGE_TASK_WORKER_INTERVAL_MILLIS);
    }

    /**
     * 断言资源节点指标。
     *
     * @param resources 资源指标响应
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertResourceNodes(Map<String, Object> resources) {
        assertThat(resources.get("nodes")).asInstanceOf(InstanceOfAssertFactories.LIST)
                .anySatisfy(row -> assertFinanceNodeMetrics(row))
                .anySatisfy(row -> assertInvoiceNodeMetrics(row));
    }

    /**
     * 断言财务节点指标。
     *
     * @param row 节点指标行
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertFinanceNodeMetrics(Object row) {
        assertThat(row).asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("node_id", FINANCE_NODE_ID)
                .containsEntry("model_key", DEFAULT_MODEL_KEY)
                .containsEntry("node_name", FINANCE_NODE_NAME)
                .containsEntry("max_concurrency", 4)
                .containsEntry("inflight_images", 0)
                .containsEntry("processed_images_today", 2L)
                .containsEntry("avg_latency_ms", 150L)
                .containsEntry("p95_latency_ms", 200L);
    }

    /**
     * 断言票据节点指标。
     *
     * @param row 节点指标行
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertInvoiceNodeMetrics(Object row) {
        assertThat(row).asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("node_id", INVOICE_NODE_ID)
                .containsEntry("node_name", INVOICE_NODE_NAME)
                .containsEntry("processed_images_today", 1L)
                .containsEntry("avg_latency_ms", 600L)
                .containsEntry("p95_latency_ms", 600L);
    }
}
