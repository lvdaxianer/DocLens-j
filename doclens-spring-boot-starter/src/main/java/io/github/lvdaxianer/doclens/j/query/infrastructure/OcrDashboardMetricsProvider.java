package io.github.lvdaxianer.doclens.j.query.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCall;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeMetrics;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrRuntimeNodePool;
import io.github.lvdaxianer.doclens.j.query.application.DashboardOcrMetricsProvider;
import java.util.Comparator;
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

    private final OcrNodeRepository nodeRepository;
    private final OcrNodeCallRepository callRepository;
    private final OcrRuntimeNodePool nodePool;
    private final DashboardThreadPools threadPools;
    private final ThreadPoolMetricsReader threadPoolMetricsReader;
    private final OcrNodeMetricsAggregator metricsAggregator;

    /**
     * 创建 OCR Dashboard 指标提供器。
     *
     * @param nodeRepository OCR 节点仓储
     * @param callRepository OCR 调用仓储
     * @param nodePool 运行时节点池
     * @param threadPools Dashboard 线程池集合
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrDashboardMetricsProvider(
            OcrNodeRepository nodeRepository,
            OcrNodeCallRepository callRepository,
            OcrRuntimeNodePool nodePool,
            DashboardThreadPools threadPools
    ) {
        this.nodeRepository = nodeRepository;
        this.callRepository = callRepository;
        this.nodePool = nodePool;
        this.threadPools = threadPools;
        this.threadPoolMetricsReader = new ThreadPoolMetricsReader();
        this.metricsAggregator = new OcrNodeMetricsAggregator(callRepository);
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
     * 获取批次实际命中节点。
     *
     * @param batchId 批次 ID
     * @return 命中节点列表
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Override
    public List<Map<String, Object>> hitNodesByBatch(String batchId) {
        Map<String, OcrNode> nodesById = nodeRepository.listAll().stream()
                .collect(Collectors.toMap(OcrNode::id, node -> node));
        return callRepository.listByBatchId(batchId).stream()
                .collect(Collectors.groupingBy(this::hitNodeKey, Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> hitNodeRow(nodesById, entry.getKey(), entry.getValue()))
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
                Map.entry("document_processing", threadPoolMetricsReader.read(threadPools.documentProcessing())),
                Map.entry("ocr_request", threadPoolMetricsReader.read(threadPools.ocrRequest())),
                Map.entry("ocr_health", threadPoolMetricsReader.read(threadPools.ocrHealth())),
                Map.entry("callback", threadPoolMetricsReader.read(threadPools.callback()))
        );
    }

    /**
     * 创建命中节点聚合键。
     *
     * @param call OCR 调用记录
     * @return 聚合键
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private HitNodeKey hitNodeKey(OcrNodeCall call) {
        return new HitNodeKey(call.modelKey(), call.nodeId());
    }

    /**
     * 创建命中节点读模型。
     *
     * @param key 命中节点聚合键
     * @param imageCount 图片数量
     * @return 命中节点读模型
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private Map<String, Object> hitNodeRow(Map<String, OcrNode> nodesById, HitNodeKey key, long imageCount) {
        return Map.ofEntries(
                Map.entry("model_key", key.modelKey()),
                Map.entry("node_id", key.nodeId()),
                Map.entry("node_name", nodeName(nodesById, key.nodeId())),
                Map.entry("image_count", imageCount)
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
     * Dashboard 线程池集合。
     *
     * @param documentProcessing 文档处理线程池
     * @param ocrRequest OCR 请求线程池
     * @param ocrHealth OCR 健康检查线程池
     * @param callback 回调线程池
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public record DashboardThreadPools(
            ExecutorService documentProcessing,
            ExecutorService ocrRequest,
            ExecutorService ocrHealth,
            ExecutorService callback
    ) {
    }

    /**
     * 命中节点聚合键。
     *
     * @param modelKey OCR 模型标识
     * @param nodeId OCR 节点 ID
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private record HitNodeKey(String modelKey, String nodeId) {
    }
}
