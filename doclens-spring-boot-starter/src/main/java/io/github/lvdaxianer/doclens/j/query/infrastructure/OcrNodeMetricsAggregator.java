package io.github.lvdaxianer.doclens.j.query.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeMetricsViewReader;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCall;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeMetrics;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrRuntimeNodePool;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OCR 节点指标聚合器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public class OcrNodeMetricsAggregator implements OcrNodeMetricsViewReader {

    private final OcrNodeCallRepository callRepository;
    private final OcrRuntimeNodePool nodePool;

    /**
     * 创建 OCR 节点指标聚合器。
     *
     * @param callRepository OCR 调用记录仓储
     * @param nodePool OCR 运行时节点池
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrNodeMetricsAggregator(OcrNodeCallRepository callRepository, OcrRuntimeNodePool nodePool) {
        this.callRepository = callRepository;
        this.nodePool = nodePool;
    }

    /**
     * 批量聚合节点当日指标。
     *
     * @param nodeIds 节点 ID 集合
     * @return 节点指标映射
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Override
    public Map<String, OcrNodeMetrics> metricsByNodeIds(List<String> nodeIds) {
        if (nodeIds.isEmpty()) {
            return Map.of();
        } else {
            LocalDate today = LocalDate.now();
            List<OcrNodeCall> calls = callRepository.listByNodeIdsAndDay(nodeIds, today).stream()
                    .filter(call -> call.startedAt().toLocalDate().equals(today))
                    .toList();
            return buildMetrics(nodeIds, calls);
        }
    }

    /**
     * 构建节点指标映射。
     *
     * @param nodeIds 节点 ID 集合
     * @param calls 调用记录集合
     * @return 节点指标映射
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private Map<String, OcrNodeMetrics> buildMetrics(List<String> nodeIds, List<OcrNodeCall> calls) {
        Map<String, List<OcrNodeCall>> callsByNodeId = calls.stream().collect(java.util.stream.Collectors.groupingBy(
                OcrNodeCall::nodeId));
        Map<String, io.github.lvdaxianer.doclens.j.adapter.application.OcrRuntimeNodeView> runtimeViews =
                runtimeViewsByNodeId();
        Map<String, OcrNodeMetrics> metricsByNodeId = new HashMap<>(nodeIds.size());
        nodeIds.forEach(nodeId -> metricsByNodeId.put(nodeId,
                metrics(callsByNodeId.getOrDefault(nodeId, List.of()), runtimeViews.get(nodeId))));
        return metricsByNodeId;
    }

    /**
     * 聚合单节点指标。
     *
     * @param calls 节点调用记录
     * @return 节点指标
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNodeMetrics metrics(
            List<OcrNodeCall> calls,
            io.github.lvdaxianer.doclens.j.adapter.application.OcrRuntimeNodeView runtimeView
    ) {
        long processedImagesToday = calls.size();
        long successImages = calls.stream().filter(call -> call.status() == OcrNodeCallStatus.SUCCESS).count();
        long failedImages = calls.stream().filter(call -> call.status() == OcrNodeCallStatus.FAILED).count();
        List<Long> elapsedTimes = calls.stream().map(OcrNodeCall::elapsedMs).sorted().toList();
        long avgLatencyMs = elapsedTimes.isEmpty()
                ? 0L
                : Math.round(elapsedTimes.stream().mapToLong(Long::longValue).average().orElse(0D));
        long p95LatencyMs = percentile95(elapsedTimes);
        Optional<java.time.OffsetDateTime> lastRequestAt = calls.stream().map(OcrNodeCall::startedAt)
                .max(java.time.OffsetDateTime::compareTo);
        Optional<String> lastError = calls.stream().filter(call -> call.status() == OcrNodeCallStatus.FAILED)
                .map(call -> call.errorMessage().orElse(""))
                .filter(message -> !message.isBlank())
                .reduce((left, right) -> right);
        return new OcrNodeMetrics(inflightImages(runtimeView), queuedImages(runtimeView), processedImagesToday,
                successImages, failedImages, avgLatencyMs, p95LatencyMs, lastRequestAt, lastError);
    }

    /**
     * 构建运行时节点索引，供真实 inflight/queued 指标读取使用。
     *
     * @return 运行时节点索引
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private Map<String, io.github.lvdaxianer.doclens.j.adapter.application.OcrRuntimeNodeView> runtimeViewsByNodeId() {
        return nodePool.snapshot().stream().collect(java.util.stream.Collectors.toMap(
                io.github.lvdaxianer.doclens.j.adapter.application.OcrRuntimeNodeView::nodeId,
                view -> view));
    }

    /**
     * 读取运行时 inflight 图片数，缺失时降级为 0。
     *
     * @param runtimeView 运行时节点视图
     * @return 运行时 inflight 图片数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private int inflightImages(io.github.lvdaxianer.doclens.j.adapter.application.OcrRuntimeNodeView runtimeView) {
        if (runtimeView == null) {
            return 0;
        } else {
            return runtimeView.inflightImages();
        }
    }

    /**
     * 读取运行时 queued 图片数，缺失时降级为 0。
     *
     * @param runtimeView 运行时节点视图
     * @return 运行时 queued 图片数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private int queuedImages(io.github.lvdaxianer.doclens.j.adapter.application.OcrRuntimeNodeView runtimeView) {
        if (runtimeView == null) {
            return 0;
        } else {
            return runtimeView.queuedImages();
        }
    }

    /**
     * 计算 P95 耗时。
     *
     * @param elapsedTimes 已排序耗时列表
     * @return P95 耗时
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private long percentile95(List<Long> elapsedTimes) {
        if (elapsedTimes.isEmpty()) {
            return 0L;
        } else {
            int index = Math.max(0, (int) Math.ceil(elapsedTimes.size() * 0.95D) - 1);
            return elapsedTimes.get(index);
        }
    }
}
