package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeDeploymentType;

/**
 * OCR 节点可配置参数。
 *
 * @param deploymentType 节点部署类型
 * @param endpoint 节点地址配置
 * @param online 在线节点配置
 * @param scheduling 节点调度配置
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record OcrNodeSettings(
        OcrNodeDeploymentType deploymentType,
        Endpoint endpoint,
        Online online,
        Scheduling scheduling
) {
    /**
     * OCR 节点地址配置。
     *
     * @param name 节点名称
     * @param host 节点主机
     * @param port 节点端口
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public record Endpoint(String name, String host, int port) {
    }

    /**
     * OCR 在线节点配置。
     *
     * @param channelKey 在线渠道标识
     * @param providerModel 在线模型名称
     * @param credentialEnvVar 在线 API Key 环境变量名
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public record Online(String channelKey, String providerModel, String credentialEnvVar) {
    }

    /**
     * OCR 节点调度配置。
     *
     * @param enabled 是否启用
     * @param participateGlobal 是否参与全局负载均衡
     * @param weight 节点权重
     * @param maxConcurrency 最大并发图片数
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public record Scheduling(boolean enabled, boolean participateGlobal, int weight, int maxConcurrency) {
    }
}
