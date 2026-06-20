package io.github.lvdaxianer.doclens.j.query.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrBatchHitTracker;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCall;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrModelDefinition;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrModelRegistry;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutingMode;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrRuntimeNodePool;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.NamedThreadPoolFactory;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * OCR Dashboard 指标测试共享支持。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
abstract class OcrDashboardMetricsProviderTestSupport {

    /*
     * 共享 support 只放无业务断言的测试基础设施。
     * 断言留在具体测试类中，保证失败信息仍然指向业务场景。
     *
     * 这些内存仓储模拟查询侧只读依赖，
     * save 类写接口保留显式异常，避免测试误用写路径。
     */

    /** 测试基准时间。 */
    protected static final OffsetDateTime BASE_TIME = OffsetDateTime.now()
            .withHour(10)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);
    /** 默认 OCR 模型键。 */
    protected static final String DEFAULT_MODEL_KEY = "paddle_ocr";
    /** 默认 OCR 模型名称。 */
    protected static final String DEFAULT_MODEL_NAME = "PaddleOCR";
    /** 财务节点 ID。 */
    protected static final String FINANCE_NODE_ID = "ocr_node_1";
    /** 财务节点名称。 */
    protected static final String FINANCE_NODE_NAME = "财务 OCR 节点";
    /** 票据节点 ID。 */
    protected static final String INVOICE_NODE_ID = "ocr_node_2";
    /** 票据节点名称。 */
    protected static final String INVOICE_NODE_NAME = "票据 OCR 节点";
    /** 合同节点 ID。 */
    protected static final String CONTRACT_NODE_ID = "ocr_node_3";
    /** 合同节点名称。 */
    protected static final String CONTRACT_NODE_NAME = "合同 OCR 节点";
    /** 测试线程保活秒数。 */
    private static final int TEST_THREAD_KEEP_ALIVE_SECONDS = 60;

    /**
     * 创建待测指标提供器。
     *
     * @param nodeRepository 节点仓储
     * @param callRepository 调用记录仓储
     * @return 指标提供器
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    protected OcrDashboardMetricsProvider provider(
            InMemoryOcrNodeRepository nodeRepository,
            InMemoryOcrNodeCallRepository callRepository
    ) {
        return provider(nodeRepository, callRepository, new InMemoryBatchHitTracker(List.of()));
    }

    /**
     * 创建带运行时命中跟踪的待测指标提供器。
     *
     * @param nodeRepository 节点仓储
     * @param callRepository 调用记录仓储
     * @param batchHitTracker 批次运行时命中跟踪器
     * @return 指标提供器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    protected OcrDashboardMetricsProvider provider(
            InMemoryOcrNodeRepository nodeRepository,
            InMemoryOcrNodeCallRepository callRepository,
            OcrBatchHitTracker batchHitTracker
    ) {
        /*
         * Provider 依赖运行时节点池提供节点快照。
         * 每个测试单独初始化节点池，避免跨测试共享状态。
         */
        OcrRuntimeNodePool nodePool = new OcrRuntimeNodePool(nodeRepository);
        nodePool.initialize();
        return new OcrDashboardMetricsProvider(new OcrDashboardMetricsProviderDependencies(
                new OcrDashboardDataSources(nodeRepository, callRepository),
                new OcrDashboardRuntimeSources(nodePool, new DashboardThreadPools(null, null)),
                new OcrDashboardAttributionSources(batchHitTracker, modelRegistry())));
    }

    /**
     * 创建带线程池指标的待测指标提供器。
     *
     * @param nodeRepository 节点仓储
     * @param callRepository 调用记录仓储
     * @param threadPools Dashboard 线程池集合
     * @return 指标提供器
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    protected OcrDashboardMetricsProvider providerWithThreadPools(
            InMemoryOcrNodeRepository nodeRepository,
            InMemoryOcrNodeCallRepository callRepository,
            DashboardThreadPools threadPools
    ) {
        OcrRuntimeNodePool nodePool = new OcrRuntimeNodePool(nodeRepository);
        nodePool.initialize();
        return new OcrDashboardMetricsProvider(new OcrDashboardMetricsProviderDependencies(
                new OcrDashboardDataSources(nodeRepository, callRepository),
                new OcrDashboardRuntimeSources(nodePool, threadPools),
                new OcrDashboardAttributionSources(new InMemoryBatchHitTracker(List.of()), modelRegistry())));
    }

    /**
     * 创建 OCR 模型注册表。
     *
     * @return OCR 模型注册表
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    protected OcrModelRegistry modelRegistry() {
        return new OcrModelRegistry(List.of(OcrModelDefinition.create(new OcrModelDefinition.CreateCommand(
                new OcrModelDefinition.Identity(DEFAULT_MODEL_KEY, DEFAULT_MODEL_NAME,
                        "PaddleOCR native-compatible HTTP API"),
                new OcrModelDefinition.Capability(List.of("image"), "/ocr", "/ocr"),
                new OcrModelDefinition.RuntimeDefaults(8080, "", "", true)))));
    }

    /**
     * 创建测试线程池。
     *
     * @param corePoolSize 核心线程数
     * @param maximumPoolSize 最大线程数
     * @param queueCapacity 队列容量
     * @return 测试线程池
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    protected ThreadPoolExecutor testExecutor(int corePoolSize, int maximumPoolSize, int queueCapacity) {
        return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, TEST_THREAD_KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS, new LinkedBlockingQueue<>(queueCapacity),
                new NamedThreadPoolFactory("doclens-test-dashboard-metrics-"));
    }

    /**
     * 关闭测试线程池。
     *
     * @param executors 线程池集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    protected void shutdownExecutors(ExecutorService... executors) {
        for (ExecutorService executor : executors) {
            executor.shutdownNow();
        }
    }

    /**
     * 创建离线测试节点。
     *
     * @param nodeId 节点 ID
     * @param name 节点名称
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    protected OcrNode offlineNode(String nodeId, String name) {
        /*
         * Dashboard 资源指标只关心节点可用性和基础容量。
         * 熔断、冷却和错误计数在这些聚合测试中保持空值。
         */
        return new OcrNode(nodeId, DEFAULT_MODEL_KEY, name, "10.0.0.1", 8080, true, true, 100, 4, OcrNodeStatus.UP,
                0L, 0L, 0L, 0L, Optional.of(BASE_TIME), Optional.empty(), Optional.empty(), Optional.empty(),
                BASE_TIME, BASE_TIME);
    }

    /**
     * 创建成功调用记录。
     *
     * @param callSeed 调用基础信息
     * @param nodeId 节点 ID
     * @param elapsedMs 耗时
     * @return 调用记录
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    protected OcrNodeCall successfulCall(CallSeed callSeed, String nodeId, long elapsedMs) {
        return new OcrNodeCall(callSeed.callId(), callSeed.batchId(), callSeed.documentId(), callSeed.pageNo(),
                DEFAULT_MODEL_KEY, nodeId, OcrRoutingMode.GLOBAL_LOAD_BALANCE, OcrNodeCallStatus.SUCCESS,
                0, elapsedMs, Optional.empty(), Optional.empty(), BASE_TIME, Optional.of(BASE_TIME.plusSeconds(1)));
    }

    /**
     * 创建失败调用记录。
     *
     * @param callSeed 调用基础信息
     * @param nodeId 节点 ID
     * @param elapsedMs 耗时
     * @return 失败调用记录
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    protected OcrNodeCall failedCall(CallSeed callSeed, String nodeId, long elapsedMs) {
        return new OcrNodeCall(callSeed.callId(), callSeed.batchId(), callSeed.documentId(), callSeed.pageNo(),
                DEFAULT_MODEL_KEY, nodeId, OcrRoutingMode.GLOBAL_LOAD_BALANCE, OcrNodeCallStatus.FAILED,
                1, elapsedMs, Optional.of("OCR_FAILED"), Optional.of("recognize failed"), BASE_TIME,
                Optional.of(BASE_TIME.plusSeconds(1)));
    }

    /**
     * 创建测试调用基础信息。
     *
     * @param callId 调用记录 ID
     * @param batchId 批次 ID
     * @param documentId 文档 ID
     * @param pageNo 页码
     * @return 调用基础信息
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    protected CallSeed callSeed(String callId, String batchId, String documentId, int pageNo) {
        return new CallSeed(callId, batchId, documentId, pageNo);
    }

    /**
     * 测试调用基础信息。
     *
     * @param callId 调用记录 ID
     * @param batchId 批次 ID
     * @param documentId 文档 ID
     * @param pageNo 页码
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    protected record CallSeed(String callId, String batchId, String documentId, int pageNo) {
    }

}
