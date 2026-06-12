package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeDeploymentType;

/**
 * 根据 OCR 节点部署类型路由健康检查。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public class RoutingOcrHealthClient implements OcrHealthClient {

    private static final String OLLAMA_CHANNEL_KEY = "ollama";
    private static final String OLLAMA_MODEL_PREFIX = "ollama";

    private final OcrHealthClient offlineHealthClient;
    private final OcrHealthClient ollamaHealthClient;
    private final DashScopeOnlineOcrClient onlineOcrClient;

    /**
     * 创建路由型 OCR 健康检查客户端。
     *
     * @param offlineHealthClient 离线 OCR 健康检查客户端
     * @param ollamaHealthClient Ollama OCR 健康检查客户端
     * @param onlineOcrClient 在线 OCR 客户端
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public RoutingOcrHealthClient(
            OcrHealthClient offlineHealthClient,
            OcrHealthClient ollamaHealthClient,
            DashScopeOnlineOcrClient onlineOcrClient
    ) {
        this.offlineHealthClient = offlineHealthClient;
        this.ollamaHealthClient = ollamaHealthClient;
        this.onlineOcrClient = onlineOcrClient;
    }

    /**
     * 判断 OCR 节点是否健康。
     *
     * @param node OCR 节点
     * @return 是否健康
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public boolean isHealthy(OcrNode node) {
        if (node.deploymentType() == OcrNodeDeploymentType.ONLINE) {
            // 在线 OCR 节点没有离线健康接口，使用在线渠道权限探测判定可用性。
            return onlineConfigurationReady(node) && onlineOcrClient.hasExecutionPermission(new OcrRuntimeNode(node));
        } else if (isOllamaNode(node)) {
            // Ollama 离线节点必须调用 /api/generate，不能复用 PaddleOCR /ocr 健康探测。
            return ollamaHealthClient.isHealthy(node);
        } else {
            // 其他离线节点按 PaddleOCR 原生协议执行 JSON/Base64 健康探测。
            return offlineHealthClient.isHealthy(node);
        }
    }

    /**
     * 判断节点是否为 Ollama OCR 节点。
     *
     * @param node OCR 节点
     * @return 是否为 Ollama OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private boolean isOllamaNode(OcrNode node) {
        return node.channelKey().filter(OLLAMA_CHANNEL_KEY::equals).isPresent()
                || node.modelKey().startsWith(OLLAMA_MODEL_PREFIX);
    }

    /**
     * 校验在线节点配置是否足以参与真实探测。
     *
     * @param node OCR 节点
     * @return 配置是否完整
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private boolean onlineConfigurationReady(OcrNode node) {
        return node.channelKey().isPresent()
                && node.providerModel().isPresent()
                && node.credentialConfigured()
                && node.credentialRef().isPresent();
    }
}
