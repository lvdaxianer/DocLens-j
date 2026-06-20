package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrModelDefinition;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeMetrics;
import java.util.List;
import java.util.Map;

/**
 * OCR 模型响应 DTO。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public class OcrModelResponse {

    private final OcrModelDefinition definition;
    private final OcrModelNodeCounts nodeCounts;

    /**
     * 创建 OCR 模型响应 DTO。
     *
     * @param definition OCR 模型定义
     * @param nodeCounts 节点统计信息
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private OcrModelResponse(OcrModelDefinition definition, OcrModelNodeCounts nodeCounts) {
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
        return new OcrModelResponse(definition, OcrModelNodeCounts.from(nodes));
    }

    /**
     * 从模型定义、节点集合与指标创建响应。
     *
     * @param definition OCR 模型定义
     * @param nodes 模型节点集合
     * @param metricsByNodeId 节点指标映射
     * @return OCR 模型响应
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    public static OcrModelResponse from(
            OcrModelDefinition definition,
            List<OcrNode> nodes,
            Map<String, OcrNodeMetrics> metricsByNodeId
    ) {
        return new OcrModelResponse(definition, OcrModelNodeCounts.from(nodes, metricsByNodeId));
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
     * 读取模型当前解析中图片数。
     *
     * @return 当前解析中图片数
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @JsonProperty("inflight_images")
    public int inflightImages() {
        return nodeCounts.inflightImages();
    }

    /**
     * 读取模型总最大并发容量。
     *
     * @return 总最大并发容量
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @JsonProperty("max_concurrency")
    public int maxConcurrency() {
        return nodeCounts.maxConcurrency();
    }

    /**
     * 读取模型启用节点并发容量。
     *
     * @return 启用节点并发容量
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @JsonProperty("enabled_max_concurrency")
    public int enabledMaxConcurrency() {
        return nodeCounts.enabledMaxConcurrency();
    }

    /**
     * 读取模型全局调度并发容量。
     *
     * @return 全局调度并发容量
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @JsonProperty("global_max_concurrency")
    public int globalMaxConcurrency() {
        return nodeCounts.globalMaxConcurrency();
    }

}
