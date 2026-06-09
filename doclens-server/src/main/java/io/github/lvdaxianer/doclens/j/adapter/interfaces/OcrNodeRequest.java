package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeSettings;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeDeploymentType;

/**
 * OCR 节点新增和编辑请求。
 *
 * @param deploymentType 节点部署类型
 * @param endpoint 节点地址参数
 * @param online 在线节点参数
 * @param scheduling 节点调度参数
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record OcrNodeRequest(
        @JsonProperty("deployment_type") OcrNodeDeploymentType deploymentType,
        @JsonUnwrapped Endpoint endpoint,
        @JsonUnwrapped Online online,
        @JsonUnwrapped Scheduling scheduling
) {
    private static final int DEFAULT_WEIGHT = 100;
    private static final int DEFAULT_MAX_CONCURRENCY = 4;

    /**
     * 转换为核心节点配置。
     *
     * @return OCR 节点配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    OcrNodeSettings toSettings() {
        return new OcrNodeSettings(effectiveDeploymentType(), endpointSettings(), onlineSettings(),
                schedulingSettings());
    }

    /**
     * 获取节点部署类型，兼容历史请求默认离线节点。
     *
     * @return 节点部署类型
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNodeDeploymentType effectiveDeploymentType() {
        return deploymentType == null ? OcrNodeDeploymentType.OFFLINE : deploymentType;
    }

    /**
     * 获取节点地址参数。
     *
     * @return 节点地址参数
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNodeSettings.Endpoint endpointSettings() {
        if (endpoint == null) {
            return new OcrNodeSettings.Endpoint("", "", 0);
        } else {
            return new OcrNodeSettings.Endpoint(endpoint.name(), endpoint.host(), endpoint.port());
        }
    }

    /**
     * 获取在线节点参数。
     *
     * @return 在线节点参数
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNodeSettings.Online onlineSettings() {
        if (online == null) {
            return new OcrNodeSettings.Online("", "", "");
        } else {
            return new OcrNodeSettings.Online(online.channelKey(), online.providerModel(), online.apiKey());
        }
    }

    /**
     * 获取节点调度参数。
     *
     * @return 节点调度参数
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNodeSettings.Scheduling schedulingSettings() {
        if (scheduling == null) {
            return new OcrNodeSettings.Scheduling(true, true, DEFAULT_WEIGHT, DEFAULT_MAX_CONCURRENCY);
        } else {
            return new OcrNodeSettings.Scheduling(scheduling.enabled(), scheduling.participateGlobal(),
                    scheduling.weight(), scheduling.maxConcurrency());
        }
    }

    /**
     * OCR 节点地址参数。
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
     * OCR 在线节点参数。
     *
     * @param channelKey 在线渠道标识
     * @param providerModel 在线模型名称
     * @param apiKey 在线 API Key
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public record Online(
            @JsonProperty("channel_key") String channelKey,
            @JsonProperty("provider_model") String providerModel,
            @JsonProperty("api_key") String apiKey
    ) {
    }

    /**
     * OCR 节点调度参数。
     *
     * @param enabled 是否启用
     * @param participateGlobal 是否参与全局负载均衡
     * @param weight 节点权重
     * @param maxConcurrency 最大并发图片数
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public record Scheduling(
            boolean enabled,
            @JsonProperty("participate_global") boolean participateGlobal,
            int weight,
            @JsonProperty("max_concurrency") int maxConcurrency
    ) {
    }
}
