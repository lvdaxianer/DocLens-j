package io.github.lvdaxianer.doclens.j.query.application;

import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import java.util.List;
import java.util.Map;

/**
 * Dashboard 批次 OCR 运行态并发快照组装器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
class DashboardOcrRuntimeCapacityAssembler {

    /**
     * 按批次路由策略创建 OCR 运行态容量快照。
     *
     * @param routePolicy 批次路由策略
     * @param ocrResources OCR 资源快照
     * @return OCR 运行态容量快照
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    Map<String, Object> assemble(Map<String, Object> routePolicy, Map<String, Object> ocrResources) {
        String modelKey = text(routePolicy.get("model_key"));
        String nodeId = text(routePolicy.get("node_id"));
        List<Map<String, Object>> nodes = matchingNodes(ocrResources, modelKey, nodeId);
        Map<String, Object> model = matchingModel(ocrResources, modelKey);
        return Map.ofEntries(
                Map.entry("model_key", valueOr(modelKey, text(model.get("model_key")))),
                Map.entry("model_name", valueOr(text(model.get("model_name")), text(model.get("name")))),
                Map.entry("inflight_images", number(model.get("inflight_images"))),
                Map.entry("max_concurrency", number(model.get("max_concurrency"))),
                Map.entry("nodes", nodes)
        );
    }

    /**
     * 筛选当前批次路由相关节点。
     *
     * @param ocrResources OCR 资源快照
     * @param modelKey OCR 模型 key
     * @param nodeId OCR 节点 ID
     * @return 节点快照
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private List<Map<String, Object>> matchingNodes(Map<String, Object> ocrResources, String modelKey, String nodeId) {
        return listOfMaps(ocrResources.get("nodes")).stream()
                .filter(node -> matches(text(node.get("model_key")), modelKey))
                .filter(node -> matches(text(node.get("node_id")), nodeId))
                .map(this::nodeRow)
                .toList();
    }

    /**
     * 查找当前批次路由相关模型。
     *
     * @param ocrResources OCR 资源快照
     * @param modelKey OCR 模型 key
     * @return 模型快照
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private Map<String, Object> matchingModel(Map<String, Object> ocrResources, String modelKey) {
        return listOfMaps(ocrResources.get("models")).stream()
                .filter(model -> matches(text(model.get("model_key")), modelKey))
                .findFirst()
                .orElseGet(Map::of);
    }

    /**
     * 创建节点容量行。
     *
     * @param node 原始节点资源行
     * @return 节点容量行
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private Map<String, Object> nodeRow(Map<String, Object> node) {
        return Map.ofEntries(
                Map.entry("model_key", text(node.get("model_key"))),
                Map.entry("node_id", text(node.get("node_id"))),
                Map.entry("node_name", text(node.get("node_name"))),
                Map.entry("inflight_images", number(node.get("inflight_images"))),
                Map.entry("max_concurrency", number(node.get("max_concurrency")))
        );
    }

    /**
     * 转换对象列表为 map 列表。
     *
     * @param value 原始值
     * @return map 列表
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private List<Map<String, Object>> listOfMaps(Object value) {
        if (value instanceof List<?> items) {
            return items.stream().filter(Map.class::isInstance)
                    .map(this::copyMap)
                    .toList();
        } else {
            return List.of();
        }
    }

    /**
     * 将任意 map 复制为字符串键的对象 map。
     *
     * @param item 原始 map
     * @return 字符串键 map
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private Map<String, Object> copyMap(Object item) {
        Map<?, ?> source = (Map<?, ?>) item;
        return source.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(entry -> text(entry.getKey()), Map.Entry::getValue));
    }

    /**
     * 判断当前值是否匹配过滤条件。
     *
     * @param value 当前值
     * @param expected 期望值
     * @return 是否匹配
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private boolean matches(String value, String expected) {
        return expected.isBlank() || expected.equals(value);
    }

    /**
     * 读取文本值。
     *
     * @param value 原始值
     * @return 文本值
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private String text(Object value) {
        return value == null ? DocLensConstants.EMPTY_VALUE : value.toString();
    }

    /**
     * 读取数值。
     *
     * @param value 原始值
     * @return 整型数值
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private int number(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    /**
     * 返回首个非空值。
     *
     * @param preferred 优先值
     * @param fallback 兜底值
     * @return 非空值
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private String valueOr(String preferred, String fallback) {
        return preferred.isBlank() ? fallback : preferred;
    }
}
