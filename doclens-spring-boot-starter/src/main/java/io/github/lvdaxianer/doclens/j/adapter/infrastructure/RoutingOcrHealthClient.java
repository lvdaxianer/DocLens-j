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

    private final OcrHealthClient offlineHealthClient;
    private final DashScopeOnlineOcrClient onlineOcrClient;

    /**
     * 创建路由型 OCR 健康检查客户端。
     *
     * @param offlineHealthClient 离线 OCR 健康检查客户端
     * @param onlineOcrClient 在线 OCR 客户端
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public RoutingOcrHealthClient(OcrHealthClient offlineHealthClient, DashScopeOnlineOcrClient onlineOcrClient) {
        this.offlineHealthClient = offlineHealthClient;
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
            return onlineConfigurationReady(node) && onlineOcrClient.hasExecutionPermission(new OcrRuntimeNode(node));
        } else {
            return offlineHealthClient.isHealthy(node);
        }
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
