package io.github.lvdaxianer.doclens.j.query.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrModelRegistry;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OCR Dashboard 命中节点行工厂。
 *
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
final class OcrDashboardHitNodeRows {

    /** OCR 模型 key 字段。 */
    private static final String MODEL_KEY_FIELD = "model_key";
    /** OCR 模型名称字段。 */
    private static final String MODEL_NAME_FIELD = "model_name";
    /** OCR 节点 ID 字段。 */
    private static final String NODE_ID_FIELD = "node_id";
    /** OCR 节点名称字段。 */
    private static final String NODE_NAME_FIELD = "node_name";
    /** 图片数量字段。 */
    private static final String IMAGE_COUNT_FIELD = "image_count";

    private final OcrModelRegistry modelRegistry;

    /**
     * 创建命中节点行工厂。
     *
     * @param modelRegistry OCR 模型注册表
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    OcrDashboardHitNodeRows(OcrModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

    /**
     * 创建排序后的命中节点读模型列表。
     *
     * @param hitCounts 命中节点统计
     * @param nodesById 节点索引
     * @return 命中节点读模型列表
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    List<Map<String, Object>> rows(Map<OcrDashboardHitNodeKey, Long> hitCounts, Map<String, OcrNode> nodesById) {
        return hitCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(OcrDashboardHitNodeKey::modelKey)
                        .thenComparing(OcrDashboardHitNodeKey::nodeId)))
                .map(entry -> row(nodesById, entry))
                .toList();
    }

    /**
     * 创建命中节点读模型。
     *
     * @param nodesById 节点索引
     * @param hitCountEntry 命中节点统计项
     * @return 命中节点读模型
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private Map<String, Object> row(
            Map<String, OcrNode> nodesById,
            Map.Entry<OcrDashboardHitNodeKey, Long> hitCountEntry
    ) {
        OcrDashboardHitNodeKey key = hitCountEntry.getKey();
        return Map.ofEntries(
                Map.entry(MODEL_KEY_FIELD, key.modelKey()),
                Map.entry(MODEL_NAME_FIELD, modelName(key.modelKey())),
                Map.entry(NODE_ID_FIELD, key.nodeId()),
                Map.entry(NODE_NAME_FIELD, nodeName(nodesById, key.nodeId())),
                Map.entry(IMAGE_COUNT_FIELD, hitCountEntry.getValue())
        );
    }

    /**
     * 返回模型名称，缺失时回退模型 key。
     *
     * @param modelKey OCR 模型标识
     * @return 模型展示名称
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private String modelName(String modelKey) {
        return modelRegistry.find(modelKey).map(model -> model.name()).orElse(modelKey);
    }

    /**
     * 返回节点名称，缺失时回退为空串。
     *
     * @param nodesById 节点索引
     * @param nodeId 节点 ID
     * @return 节点名称
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private String nodeName(Map<String, OcrNode> nodesById, String nodeId) {
        return Optional.ofNullable(nodesById.get(nodeId)).map(OcrNode::name).orElse("");
    }
}
