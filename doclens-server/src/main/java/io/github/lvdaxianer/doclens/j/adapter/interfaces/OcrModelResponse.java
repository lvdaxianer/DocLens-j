package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrModelDefinition;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import java.util.List;

/**
 * OCR 模型响应 DTO。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record OcrModelResponse(
        @JsonProperty("model_key") String modelKey,
        String name,
        String description,
        @JsonProperty("supported_inputs") List<String> supportedInputs,
        @JsonProperty("ocr_path") String ocrPath,
        @JsonProperty("health_path") String healthPath,
        @JsonProperty("node_count") int nodeCount,
        @JsonProperty("healthy_node_count") int healthyNodeCount,
        @JsonProperty("enabled_node_count") int enabledNodeCount
) {

    /**
     * 从模型定义与节点集合创建响应。
     *
     * @param definition OCR 模型定义
     * @param nodes 模型节点集合
     * @return OCR 模型响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public static OcrModelResponse from(OcrModelDefinition definition, List<OcrNode> nodes) {
        return new OcrModelResponse(definition.modelKey(), definition.name(), definition.description(),
                definition.supportedInputs(), definition.ocrPath(), definition.healthPath(), nodes.size(),
                healthyNodeCount(nodes), enabledNodeCount(nodes));
    }

    /**
     * 统计健康节点数量。
     *
     * @param nodes OCR 节点集合
     * @return 健康节点数量
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static int healthyNodeCount(List<OcrNode> nodes) {
        return (int) nodes.stream().filter(node -> node.status() == OcrNodeStatus.UP).count();
    }

    /**
     * 统计启用节点数量。
     *
     * @param nodes OCR 节点集合
     * @return 启用节点数量
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static int enabledNodeCount(List<OcrNode> nodes) {
        return (int) nodes.stream().filter(OcrNode::enabled).count();
    }
}
