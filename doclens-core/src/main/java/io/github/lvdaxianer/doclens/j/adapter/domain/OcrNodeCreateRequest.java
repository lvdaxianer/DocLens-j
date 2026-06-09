package io.github.lvdaxianer.doclens.j.adapter.domain;

import java.time.OffsetDateTime;

/**
 * OCR 节点创建请求。
 *
 * @param id 节点 ID
 * @param modelKey OCR 模型标识
 * @param deploymentType 节点部署类型
 * @param name 节点名称
 * @param host 节点主机
 * @param port 节点端口
 * @param channelKey 在线渠道标识
 * @param providerModel 在线模型名称
 * @param credentialRef 在线凭证引用
 * @param credentialConfigured 是否已配置在线凭证
 * @param enabled 是否启用
 * @param participateGlobal 是否参与全局负载均衡
 * @param weight 节点权重
 * @param maxConcurrency 最大并发图片数
 * @param now 当前时间
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record OcrNodeCreateRequest(
        String id,
        String modelKey,
        OcrNodeDeploymentType deploymentType,
        String name,
        String host,
        int port,
        String channelKey,
        String providerModel,
        String credentialRef,
        boolean credentialConfigured,
        boolean enabled,
        boolean participateGlobal,
        int weight,
        int maxConcurrency,
        OffsetDateTime now
) {
    /**
     * 创建兼容旧离线节点参数的请求。
     *
     * @param id 节点 ID
     * @param modelKey OCR 模型标识
     * @param name 节点名称
     * @param host 节点主机
     * @param port 节点端口
     * @param enabled 是否启用
     * @param participateGlobal 是否参与全局负载均衡
     * @param weight 节点权重
     * @param maxConcurrency 最大并发图片数
     * @param now 当前时间
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrNodeCreateRequest(
            String id,
            String modelKey,
            String name,
            String host,
            int port,
            boolean enabled,
            boolean participateGlobal,
            int weight,
            int maxConcurrency,
            OffsetDateTime now
    ) {
        this(id, modelKey, OcrNodeDeploymentType.OFFLINE, name, host, port, "", "", "", false, enabled,
                participateGlobal, weight, maxConcurrency, now);
    }
}
