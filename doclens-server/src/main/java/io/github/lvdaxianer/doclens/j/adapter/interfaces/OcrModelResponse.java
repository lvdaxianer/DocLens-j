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
public class OcrModelResponse {

    private final OcrModelDefinition definition;
    private final NodeCounts nodeCounts;

    /**
     * 创建 OCR 模型响应 DTO。
     *
     * @param definition OCR 模型定义
     * @param nodeCounts 节点统计信息
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private OcrModelResponse(OcrModelDefinition definition, NodeCounts nodeCounts) {
        this.definition = definition;
        this.nodeCounts = nodeCounts;
    }

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
        return new OcrModelResponse(definition, NodeCounts.from(nodes));
    }

    /**
     * 读取模型标识。
     *
     * @return 模型标识
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @JsonProperty("model_key")
    public String modelKey() {
        return definition.modelKey();
    }

    /**
     * 读取模型名称。
     *
     * @return 模型名称
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @JsonProperty("name")
    public String name() {
        return definition.name();
    }

    /**
     * 读取模型描述。
     *
     * @return 模型描述
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public String description() {
        return definition.description();
    }

    /**
     * 读取支持输入类型。
     *
     * @return 支持输入类型
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @JsonProperty("supported_inputs")
    public List<String> supportedInputs() {
        return definition.supportedInputs();
    }

    /**
     * 读取 OCR 固定路径。
     *
     * @return OCR 固定路径
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @JsonProperty("ocr_path")
    public String ocrPath() {
        return definition.ocrPath();
    }

    /**
     * 读取健康检查固定路径。
     *
     * @return 健康检查固定路径
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @JsonProperty("health_path")
    public String healthPath() {
        return definition.healthPath();
    }

    /**
     * 读取默认端口。
     *
     * @return 默认端口
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @JsonProperty("default_port")
    public int defaultPort() {
        return definition.defaultPort();
    }

    /**
     * 读取默认厂商模型。
     *
     * @return 默认厂商模型
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @JsonProperty("provider_model")
    public String providerModel() {
        return definition.providerModel();
    }

    /**
     * 读取默认通道标识。
     *
     * @return 默认通道标识
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @JsonProperty("channel_key")
    public String channelKey() {
        return definition.channelKey();
    }

    /**
     * 读取节点数量。
     *
     * @return 节点数量
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @JsonProperty("node_count")
    public int nodeCount() {
        return nodeCounts.nodeCount();
    }

    /**
     * 读取健康节点数量。
     *
     * @return 健康节点数量
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @JsonProperty("healthy_node_count")
    public int healthyNodeCount() {
        return nodeCounts.healthyNodeCount();
    }

    /**
     * 读取启用节点数量。
     *
     * @return 启用节点数量
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @JsonProperty("enabled_node_count")
    public int enabledNodeCount() {
        return nodeCounts.enabledNodeCount();
    }

    /**
     * OCR 节点统计信息。
     *
     * @param nodeCount 节点数量
     * @param healthyNodeCount 健康节点数量
     * @param enabledNodeCount 启用节点数量
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private record NodeCounts(int nodeCount, int healthyNodeCount, int enabledNodeCount) {

        /**
         * 从节点集合创建统计信息。
         *
         * @param nodes OCR 节点集合
         * @return 节点统计信息
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        private static NodeCounts from(List<OcrNode> nodes) {
            return new NodeCounts(nodes.size(), OcrModelResponse.healthyNodeCount(nodes),
                    OcrModelResponse.enabledNodeCount(nodes));
        }
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
