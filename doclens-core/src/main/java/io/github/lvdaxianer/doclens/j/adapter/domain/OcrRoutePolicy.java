package io.github.lvdaxianer.doclens.j.adapter.domain;

import java.util.Optional;

/**
 * 批次级 OCR 路由策略。
 *
 * @param routingMode OCR 路由模式
 * @param modelKey 指定 OCR 模型标识
 * @param nodeId 指定 OCR 节点标识
 * @param loadBalanceStrategy 负载均衡策略
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record OcrRoutePolicy(
        OcrRoutingMode routingMode,
        Optional<String> modelKey,
        Optional<String> nodeId,
        Optional<String> loadBalanceStrategy
) {

    /**
     * 创建 OCR 路由策略。
     *
     * @param routingMode OCR 路由模式
     * @param modelKey 指定 OCR 模型标识
     * @param nodeId 指定 OCR 节点标识
     * @param loadBalanceStrategy 负载均衡策略
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public OcrRoutePolicy {
        routingMode = requiredRoutingMode(routingMode);
        modelKey = normalize(modelKey);
        nodeId = normalize(nodeId);
        loadBalanceStrategy = normalize(loadBalanceStrategy);
    }

    /**
     * 创建系统默认路由策略。
     *
     * @return 系统默认路由策略
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public static OcrRoutePolicy defaultPolicy() {
        return new OcrRoutePolicy(OcrRoutingMode.DEFAULT, Optional.empty(), Optional.empty(), Optional.empty());
    }

    /**
     * 创建全局负载均衡路由策略。
     *
     * @param strategy 负载均衡策略
     * @return 全局负载均衡路由策略
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public static OcrRoutePolicy globalLoadBalance(String strategy) {
        return new OcrRoutePolicy(OcrRoutingMode.GLOBAL_LOAD_BALANCE, Optional.empty(), Optional.empty(),
                Optional.ofNullable(strategy));
    }

    /**
     * 创建指定 OCR 模型负载均衡路由策略。
     *
     * @param modelKey OCR 模型标识
     * @param strategy 负载均衡策略
     * @return 指定 OCR 模型负载均衡路由策略
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public static OcrRoutePolicy modelLoadBalance(String modelKey, String strategy) {
        String normalizedModelKey = requiredText(modelKey, "ocr model key is required");
        return new OcrRoutePolicy(OcrRoutingMode.MODEL_LOAD_BALANCE, Optional.of(normalizedModelKey),
                Optional.empty(), Optional.ofNullable(strategy));
    }

    /**
     * 创建固定 OCR 节点路由策略。
     *
     * @param modelKey OCR 模型标识
     * @param nodeId OCR 节点标识
     * @return 固定 OCR 节点路由策略
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public static OcrRoutePolicy specificNode(String modelKey, String nodeId) {
        String normalizedModelKey = requiredText(modelKey, "ocr model key is required");
        String normalizedNodeId = requiredText(nodeId, "ocr node id is required");
        return new OcrRoutePolicy(OcrRoutingMode.SPECIFIC_NODE, Optional.of(normalizedModelKey),
                Optional.of(normalizedNodeId), Optional.empty());
    }

    /**
     * 校验路由模式不能为空。
     *
     * @param routingMode OCR 路由模式
     * @return 非空 OCR 路由模式
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static OcrRoutingMode requiredRoutingMode(OcrRoutingMode routingMode) {
        if (routingMode != null) {
            return routingMode;
        } else {
            throw new IllegalArgumentException("ocr routing mode is required");
        }
    }

    /**
     * 标准化可选文本。
     *
     * @param value 可选文本
     * @return 标准化后的可选文本
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static Optional<String> normalize(Optional<String> value) {
        return value.flatMap(OcrRoutePolicy::presentText);
    }

    /**
     * 保留非空白文本。
     *
     * @param value 文本值
     * @return 非空白可选文本
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static Optional<String> presentText(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        } else {
            return Optional.of(value.trim());
        }
    }

    /**
     * 获取必填文本。
     *
     * @param value 文本值
     * @param message 校验失败消息
     * @return 标准化后的必填文本
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static String requiredText(String value, String message) {
        return presentText(value).orElseThrow(() -> new IllegalArgumentException(message));
    }
}
