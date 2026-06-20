package io.github.lvdaxianer.doclens.j.query.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrBatchHitTracker;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrBatchNodeHit;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCall;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeMetrics;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrRuntimeNodePool;
import io.github.lvdaxianer.doclens.j.query.application.DashboardOcrMetricsProvider;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * OCR Dashboard 资源指标提供器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public class OcrDashboardMetricsProvider implements DashboardOcrMetricsProvider {

    /** 文档处理线程池指标键。 */
    private static final String DOCUMENT_PROCESSING_POOL_KEY = "document_processing";
    /** OCR 请求线程池指标键。 */
    private static final String OCR_REQUEST_POOL_KEY = "ocr_request";
    /** OCR 健康检查线程池指标键。 */
    private static final String OCR_HEALTH_POOL_KEY = "ocr_health";
    /** 回调线程池指标键。 */
    private static final String CALLBACK_POOL_KEY = "callback";
    /** LLM Markdown 分块线程池指标键。 */
    private static final String LLM_MARKDOWN_CHUNK_POOL_KEY = "llm_markdown_chunk";
    /** 页任务 worker 指标键。 */
    private static final String PAGE_TASK_WORKER_POOL_KEY = "page_task_worker";
    /** 当前运行时线程池大小指标键。 */
    private static final String RUNTIME_POOL_SIZE_KEY = "runtime_pool_size";
    /** 线程池大小指标键。 */
    private static final String POOL_SIZE_KEY = "pool_size";
    /** 页任务批量大小指标键。 */
    private static final String BATCH_SIZE_KEY = "batch_size";
    /** 页任务锁秒数指标键。 */
    private static final String LOCK_SECONDS_KEY = "lock_seconds";
    /** 队列容量指标键。 */
    private static final String QUEUE_CAPACITY_KEY = "queue_capacity";
    /** 恢复数量上限指标键。 */
    private static final String RECOVERY_LIMIT_KEY = "recovery_limit";
    /** 调度间隔毫秒指标键。 */
    private static final String INTERVAL_MILLIS_KEY = "interval_millis";

    private final OcrNodeRepository nodeRepository;
    private final OcrNodeCallRepository callRepository;
    private final OcrRuntimeNodePool nodePool;
    private final DashboardThreadPools threadPools;
    private final ThreadPoolMetricsReader threadPoolMetricsReader;
    private final OcrNodeMetricsAggregator metricsAggregator;
    private final OcrBatchHitTracker batchHitTracker;

    /**
     * 创建 OCR Dashboard 指标提供器。
     *
     * @param nodeRepository OCR 节点仓储
     * @param callRepository OCR 调用仓储
     * @param nodePool 运行时节点池
     * @param threadPools Dashboard 线程池集合
     * @param batchHitTracker 批次运行时命中跟踪器
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrDashboardMetricsProvider(
            OcrNodeRepository nodeRepository,
            OcrNodeCallRepository callRepository,
            OcrRuntimeNodePool nodePool,
            DashboardThreadPools threadPools,
            OcrBatchHitTracker batchHitTracker
    ) {
        this.nodeRepository = nodeRepository;
        this.callRepository = callRepository;
        this.nodePool = nodePool;
        this.threadPools = threadPools;
        this.threadPoolMetricsReader = new ThreadPoolMetricsReader();
        this.metricsAggregator = new OcrNodeMetricsAggregator(callRepository, nodePool);
        this.batchHitTracker = batchHitTracker;
    }

    /**
     * 获取 OCR 资源指标。
     *
     * @return OCR 资源指标
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Override
    public Map<String, Object> ocrResources() {
        List<OcrNode> nodes = nodeRepository.listAll();
        Map<String, OcrNodeMetrics> metricsByNodeId = metricsAggregator.metricsByNodeIds(
                nodes.stream().map(OcrNode::id).toList());
        return Map.ofEntries(
                Map.entry("healthy_node_count", statusCount(nodes, OcrNodeStatus.UP)),
                Map.entry("down_node_count", statusCount(nodes, OcrNodeStatus.DOWN)),
                Map.entry("recovering_node_count", statusCount(nodes, OcrNodeStatus.RECOVERING)),
                Map.entry("global_inflight_images", globalInflightImages()),
                Map.entry("busiest_node", busiestNode()),
                Map.entry("nodes", nodeRows(nodes, metricsByNodeId)),
                Map.entry("thread_pools", threadPoolMetrics())
        );
    }

    /**
     * 获取批次级 OCR 调度命中节点。
     *
     * @param batchId 批次 ID
     * @return 命中节点列表
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public List<Map<String, Object>> dispatchHitNodesByBatch(String batchId) {
        Map<String, OcrNode> nodesById = nodeRepository.listAll().stream()
                .collect(Collectors.toMap(OcrNode::id, node -> node));
        Map<OcrDashboardHitNodeKey, Long> hitCounts = callRepository.listByBatchId(batchId).stream()
                .collect(Collectors.groupingBy(this::hitNodeKey, Collectors.counting()));
        mergeRuntimeHits(hitCounts, batchHitTracker.snapshotByBatch(batchId));
        return hitCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(OcrDashboardHitNodeKey::modelKey)
                        .thenComparing(OcrDashboardHitNodeKey::nodeId)))
                .map(entry -> hitNodeRow(nodesById, entry))
                .toList();
    }

    /**
     * 获取批次内各文档最终成功分配到的 OCR 节点。
     *
     * @param batchId 批次 ID
     * @return 文档最终分配节点映射
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public Map<String, List<Map<String, Object>>> finalHitNodesByBatch(String batchId) {
        Map<String, OcrNode> nodesById = nodeRepository.listAll().stream()
                .collect(Collectors.toMap(OcrNode::id, node -> node));
        Map<String, List<OcrNodeCall>> callsByDocumentId = callRepository.listByBatchId(batchId).stream()
                .filter(call -> call.status() == OcrNodeCallStatus.SUCCESS)
                .collect(Collectors.groupingBy(OcrNodeCall::documentId));
        return callsByDocumentId.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> finalHitNodes(entry.getValue(), nodesById)));
    }

    /**
     * 合并运行时命中快照，补齐尚未落库的处理中图片分布。
     *
     * @param hitCounts 已落库命中统计
     * @param runtimeHits 运行时命中快照
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void mergeRuntimeHits(Map<OcrDashboardHitNodeKey, Long> hitCounts, List<OcrBatchNodeHit> runtimeHits) {
        runtimeHits.forEach(hit -> hitCounts.merge(new OcrDashboardHitNodeKey(hit.modelKey(), hit.nodeId()),
                hit.imageCount(), Long::sum));
    }

    /**
     * 同一页存在多次成功调用时，保留最终完成的那次成功归属。
     *
     * @param current 已保留的成功调用
     * @param candidate 候选成功调用
     * @return 最终成功调用
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrNodeCall latestSuccessfulCallByPage(OcrNodeCall current, OcrNodeCall candidate) {
        OffsetDateTime currentFinishedAt = current.finishedAt().orElse(current.startedAt());
        OffsetDateTime candidateFinishedAt = candidate.finishedAt().orElse(candidate.startedAt());
        if (candidateFinishedAt.isAfter(currentFinishedAt)) {
            // 候选调用完成时间更新时，将最终归属更新为候选调用。
            return candidate;
        } else {
            // 已保留调用仍是最新完成结果时，继续保持当前归属。
            return current;
        }
    }

    /**
     * 从同一文档的成功调用中组装最终命中节点。
     *
     * @param successfulCalls 文档成功调用集合
     * @param nodesById 节点索引
     * @return 文档最终命中节点
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private List<Map<String, Object>> finalHitNodes(List<OcrNodeCall> successfulCalls, Map<String, OcrNode> nodesById) {
        Map<Integer, OcrNodeCall> finalSuccessfulCallsByPage = successfulCalls.stream()
                .collect(Collectors.toMap(OcrNodeCall::pageNo, call -> call, this::latestSuccessfulCallByPage,
                        LinkedHashMap::new));
        Map<OcrDashboardHitNodeKey, Long> hitCounts = finalSuccessfulCallsByPage.values().stream()
                .collect(Collectors.groupingBy(this::hitNodeKey, Collectors.counting()));
        return hitCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(OcrDashboardHitNodeKey::modelKey)
                        .thenComparing(OcrDashboardHitNodeKey::nodeId)))
                .map(entry -> hitNodeRow(nodesById, entry))
                .toList();
    }

    /**
     * 统计指定节点状态数量。
     *
     * @param nodes OCR 节点集合
     * @param status 节点状态
     * @return 节点数量
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private long statusCount(List<OcrNode> nodes, OcrNodeStatus status) {
        return nodes.stream().filter(node -> node.status() == status).count();
    }

    /**
     * 统计全局解析中图片数量。
     *
     * @return 解析中图片数量
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private long globalInflightImages() {
        return nodePool.snapshot().stream().mapToLong(node -> node.inflightImages()).sum();
    }

    /**
     * 获取最繁忙节点。
     *
     * @return 最繁忙节点读模型
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private Map<String, Object> busiestNode() {
        return nodePool.snapshot().stream()
                .max(Comparator.comparingInt(node -> node.inflightImages()))
                .map(node -> Map.<String, Object>of(
                        "node_id", node.nodeId(),
                        "model_key", node.modelKey(),
                        "inflight_images", node.inflightImages()))
                .orElseGet(Map::of);
    }

    /**
     * 组装 OCR 节点表展示指标。
     *
     * @param nodes OCR 节点集合
     * @param metricsByNodeId 节点指标映射
     * @return 节点展示指标
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private List<Map<String, Object>> nodeRows(List<OcrNode> nodes, Map<String, OcrNodeMetrics> metricsByNodeId) {
        return nodes.stream().map(node -> nodeRow(node, metricsByNodeId)).toList();
    }

    /**
     * 组装单个 OCR 节点展示指标。
     *
     * @param node OCR 节点
     * @param metricsByNodeId 节点指标映射
     * @return 节点展示指标
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private Map<String, Object> nodeRow(OcrNode node, Map<String, OcrNodeMetrics> metricsByNodeId) {
        OcrNodeMetrics metrics = metricsByNodeId.getOrDefault(node.id(),
                new OcrNodeMetrics(0, 0, 0L, 0L, 0L, 0L, 0L, Optional.empty(), Optional.empty()));
        return Map.ofEntries(
                Map.entry("node_id", node.id()),
                Map.entry("node_name", node.name()),
                Map.entry("status", node.status().name()),
                Map.entry("last_health_at", node.lastHealthAt().map(OffsetDateTime::toString).orElse("")),
                Map.entry("last_error", node.lastError().orElse("")),
                Map.entry("processed_images_today", metrics.processedImagesToday()),
                Map.entry("success_images", metrics.successImages()),
                Map.entry("failed_images", metrics.failedImages()),
                Map.entry("avg_latency_ms", metrics.avgLatencyMs()),
                Map.entry("p95_latency_ms", metrics.p95LatencyMs())
        );
    }

    /**
     * 获取线程池指标。
     *
     * @return 线程池指标
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private Map<String, Object> threadPoolMetrics() {
        return Map.ofEntries(
                Map.entry(DOCUMENT_PROCESSING_POOL_KEY, threadPools.documentProcessing()
                        .map(this::readThreadPoolMetrics).orElseGet(threadPoolMetricsReader::emptyMetrics)),
                Map.entry(OCR_REQUEST_POOL_KEY, threadPools.ocrRequest()
                        .map(this::readThreadPoolMetrics).orElseGet(threadPoolMetricsReader::emptyMetrics)),
                Map.entry(OCR_HEALTH_POOL_KEY, threadPools.ocrHealth()
                        .map(this::readThreadPoolMetrics).orElseGet(threadPoolMetricsReader::emptyMetrics)),
                Map.entry(CALLBACK_POOL_KEY, threadPools.callback()
                        .map(this::readThreadPoolMetrics).orElseGet(threadPoolMetricsReader::emptyMetrics)),
                Map.entry(LLM_MARKDOWN_CHUNK_POOL_KEY, threadPools.llmMarkdownChunk()
                        .map(this::readThreadPoolMetrics).orElseGet(threadPoolMetricsReader::emptyMetrics)),
                Map.entry(PAGE_TASK_WORKER_POOL_KEY, pageTaskWorkerMetrics())
        );
    }

    /**
     * 读取线程池指标。
     *
     * @param executorService 线程池
     * @return 线程池指标
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private Map<String, Object> readThreadPoolMetrics(ExecutorService executorService) {
        return threadPoolMetricsReader.read(executorService);
    }

    /**
     * 获取页任务 worker 指标。
     *
     * @return 页任务 worker 指标
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private Map<String, Object> pageTaskWorkerMetrics() {
        Map<String, Object> metrics =
                new LinkedHashMap<>(threadPools.pageTaskWorkerExecutor()
                        .map(this::readThreadPoolMetrics).orElseGet(threadPoolMetricsReader::emptyMetrics));
        metrics.put(RUNTIME_POOL_SIZE_KEY, metrics.get(POOL_SIZE_KEY));
        threadPools.pageTaskWorkerSettings().ifPresent(settings -> metrics.putAll(settings.toMetrics()));
        return Map.copyOf(metrics);
    }

    /**
     * 创建命中节点聚合键。
     *
     * @param call OCR 调用记录
     * @return 聚合键
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrDashboardHitNodeKey hitNodeKey(OcrNodeCall call) {
        return new OcrDashboardHitNodeKey(call.modelKey(), call.nodeId());
    }

    /**
     * 创建命中节点读模型。
     *
     * @param nodesById 节点索引
     * @param hitCountEntry 命中节点统计项
     * @return 命中节点读模型
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private Map<String, Object> hitNodeRow(
            Map<String, OcrNode> nodesById,
            Map.Entry<OcrDashboardHitNodeKey, Long> hitCountEntry
    ) {
        OcrDashboardHitNodeKey key = hitCountEntry.getKey();
        return Map.ofEntries(
                Map.entry("model_key", key.modelKey()),
                Map.entry("node_id", key.nodeId()),
                Map.entry("node_name", nodeName(nodesById, key.nodeId())),
                Map.entry("image_count", hitCountEntry.getValue())
        );
    }

    /**
     * 返回节点名称，缺失时回退为空串。
     *
     * @param nodesById 节点索引
     * @param nodeId 节点 ID
     * @return 节点名称
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String nodeName(Map<String, OcrNode> nodesById, String nodeId) {
        return Optional.ofNullable(nodesById.get(nodeId)).map(OcrNode::name).orElse("");
    }

    /**
     * Dashboard 核心线程池集合。
     *
     * @param documentProcessing 文档处理线程池
     * @param ocrRequest OCR 请求线程池
     * @param ocrHealth OCR 健康检查线程池
     * @param callback 回调线程池
     * @param llmMarkdownChunk LLM Markdown 分块线程池
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    public record DashboardCoreThreadPools(
            ExecutorService documentProcessing,
            ExecutorService ocrRequest,
            ExecutorService ocrHealth,
            ExecutorService callback,
            ExecutorService llmMarkdownChunk
    ) {
    }

    /**
     * Dashboard 线程池集合。
     *
     * @param core 核心线程池集合
     * @param pageTaskWorkerExecutor 页任务 OCR 执行线程池
     * @param pageTaskWorkerSettings 页任务 worker 生效配置
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    public record DashboardThreadPools(
            DashboardCoreThreadPools core,
            Optional<ExecutorService> pageTaskWorkerExecutor,
            Optional<DashboardPageTaskWorkerSettings> pageTaskWorkerSettings
    ) {

        /**
         * 创建 Dashboard 线程池集合。
         *
         * @param core 核心线程池集合
         * @param pageTaskWorkerExecutor 页任务 OCR 执行线程池
         * @param pageTaskWorkerSettings 页任务 worker 生效配置
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        public DashboardThreadPools {
            pageTaskWorkerExecutor = pageTaskWorkerExecutor == null ? Optional.empty() : pageTaskWorkerExecutor;
            pageTaskWorkerSettings = pageTaskWorkerSettings == null ? Optional.empty() : pageTaskWorkerSettings;
        }

        /**
         * 创建无页任务 worker 配置的 Dashboard 线程池集合。
         *
         * @param core 核心线程池集合
         * @param pageTaskWorkerExecutor 页任务 OCR 执行线程池
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        public DashboardThreadPools(
                DashboardCoreThreadPools core,
                ExecutorService pageTaskWorkerExecutor
        ) {
            this(core, Optional.ofNullable(pageTaskWorkerExecutor), Optional.empty());
        }

        /**
         * 获取文档处理线程池。
         *
         * @return 文档处理线程池
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        private Optional<ExecutorService> documentProcessing() {
            return Optional.ofNullable(core).map(DashboardCoreThreadPools::documentProcessing);
        }

        /**
         * 获取 OCR 请求线程池。
         *
         * @return OCR 请求线程池
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        private Optional<ExecutorService> ocrRequest() {
            return Optional.ofNullable(core).map(DashboardCoreThreadPools::ocrRequest);
        }

        /**
         * 获取 OCR 健康检查线程池。
         *
         * @return OCR 健康检查线程池
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        private Optional<ExecutorService> ocrHealth() {
            return Optional.ofNullable(core).map(DashboardCoreThreadPools::ocrHealth);
        }

        /**
         * 获取回调线程池。
         *
         * @return 回调线程池
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        private Optional<ExecutorService> callback() {
            return Optional.ofNullable(core).map(DashboardCoreThreadPools::callback);
        }

        /**
         * 获取 LLM Markdown 分块线程池。
         *
         * @return LLM Markdown 分块线程池
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        private Optional<ExecutorService> llmMarkdownChunk() {
            return Optional.ofNullable(core).map(DashboardCoreThreadPools::llmMarkdownChunk);
        }
    }

    /**
     * 页任务 worker 生效配置。
     *
     * @param execution 页任务 worker 执行配置
     * @param recovery 页任务 worker 恢复配置
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    public record DashboardPageTaskWorkerSettings(
            Execution execution,
            Recovery recovery
    ) {

        /**
         * 转换为 Dashboard 指标键值。
         *
         * @return Dashboard 指标键值
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        private Map<String, Object> toMetrics() {
            return Map.ofEntries(
                    Map.entry(BATCH_SIZE_KEY, execution.batchSize()),
                    Map.entry(LOCK_SECONDS_KEY, execution.lockSeconds()),
                    Map.entry(POOL_SIZE_KEY, execution.poolSize()),
                    Map.entry(QUEUE_CAPACITY_KEY, execution.queueCapacity()),
                    Map.entry(RECOVERY_LIMIT_KEY, recovery.recoveryLimit()),
                    Map.entry(INTERVAL_MILLIS_KEY, recovery.intervalMillis())
            );
        }

        /**
         * 页任务 worker 执行配置。
         *
         * @param batchSize 每轮抢占页任务数量
         * @param lockSeconds 页任务锁秒数
         * @param poolSize 页任务执行线程数
         * @param queueCapacity 页任务执行队列容量
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        public record Execution(
                int batchSize,
                int lockSeconds,
                int poolSize,
                int queueCapacity
        ) {
        }

        /**
         * 页任务 worker 恢复配置。
         *
         * @param recoveryLimit 每轮恢复过期页任务数量
         * @param intervalMillis 页任务扫描间隔毫秒
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        public record Recovery(int recoveryLimit, int intervalMillis) {
        }
    }

}
