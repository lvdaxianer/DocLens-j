package io.github.lvdaxianer.doclens.j.query.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrBatchNodeHit;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCall;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OCR Dashboard 文档级命中节点组装器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
final class OcrDashboardDocumentHitRows {

    private final OcrDashboardHitNodeRows hitNodeRows;

    /**
     * 创建 OCR Dashboard 文档级命中节点组装器。
     *
     * @param hitNodeRows 命中节点行工厂
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    OcrDashboardDocumentHitRows(OcrDashboardHitNodeRows hitNodeRows) {
        this.hitNodeRows = hitNodeRows;
    }

    /**
     * 获取批次内各文档正在处理中的 OCR 分配节点。
     *
     * @param runtimeHits 运行中命中集合
     * @param nodesById 节点索引
     * @return 文档运行中分配节点映射
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    Map<String, List<Map<String, Object>>> runningHitNodes(List<OcrBatchNodeHit> runtimeHits,
            Map<String, OcrNode> nodesById) {
        return runtimeHits.stream()
                .collect(Collectors.groupingBy(OcrBatchNodeHit::documentId))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> runtimeHitNodes(entry.getValue(), nodesById)));
    }

    /**
     * 获取批次内各文档最终成功分配到的 OCR 节点。
     *
     * @param callsByDocumentId 文档成功调用映射
     * @param nodesById 节点索引
     * @return 文档最终分配节点映射
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    Map<String, List<Map<String, Object>>> finalHitNodes(
            Map<String, List<OcrNodeCall>> callsByDocumentId,
            Map<String, OcrNode> nodesById
    ) {
        return callsByDocumentId.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> finalHitNodes(entry.getValue(), nodesById)));
    }

    /**
     * 组装指定文档的运行中命中节点行。
     *
     * @param runtimeHits 运行中命中集合
     * @param nodesById 节点索引
     * @return 运行中命中节点行
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private List<Map<String, Object>> runtimeHitNodes(List<OcrBatchNodeHit> runtimeHits,
            Map<String, OcrNode> nodesById) {
        Map<OcrDashboardHitNodeKey, Long> hitCounts = runtimeHits.stream()
                .collect(Collectors.groupingBy(hit -> new OcrDashboardHitNodeKey(hit.modelKey(), hit.nodeId()),
                        Collectors.summingLong(OcrBatchNodeHit::imageCount)));
        return hitNodeRows.rows(hitCounts, nodesById);
    }

    /**
     * 从同一文档的成功调用中组装最终命中节点。
     *
     * @param successfulCalls 文档成功调用集合
     * @param nodesById 节点索引
     * @return 文档最终命中节点
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private List<Map<String, Object>> finalHitNodes(List<OcrNodeCall> successfulCalls, Map<String, OcrNode> nodesById) {
        Map<Integer, OcrNodeCall> finalCallsByPage = successfulCalls.stream()
                .collect(Collectors.toMap(OcrNodeCall::pageNo, call -> call, this::latestSuccessfulCallByPage,
                        LinkedHashMap::new));
        Map<OcrDashboardHitNodeKey, Long> hitCounts = finalCallsByPage.values().stream()
                .collect(Collectors.groupingBy(this::hitNodeKey, Collectors.counting()));
        return hitNodeRows.rows(hitCounts, nodesById);
    }

    /**
     * 同一页存在多次成功调用时，保留最终完成的那次成功归属。
     *
     * @param current 已保留的成功调用
     * @param candidate 候选成功调用
     * @return 最终成功调用
     * @author lvdaxianerplus
     * @date 2026-06-21
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
     * 创建命中节点聚合键。
     *
     * @param call OCR 节点调用
     * @return 命中节点聚合键
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private OcrDashboardHitNodeKey hitNodeKey(OcrNodeCall call) {
        return new OcrDashboardHitNodeKey(call.modelKey(), call.nodeId());
    }
}
