package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeSettings;

/**
 * OCR 节点新增和编辑请求。
 *
 * @param endpoint 节点地址参数
 * @param scheduling 节点调度参数
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record OcrNodeRequest(
        @JsonUnwrapped Endpoint endpoint,
        @JsonUnwrapped Scheduling scheduling
) {

    /**
     * 转换为核心节点配置。
     *
     * @return OCR 节点配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    OcrNodeSettings toSettings() {
        return new OcrNodeSettings(new OcrNodeSettings.Endpoint(endpoint.name(), endpoint.host(), endpoint.port()),
                new OcrNodeSettings.Scheduling(scheduling.enabled(), scheduling.participateGlobal(),
                        scheduling.weight(), scheduling.maxConcurrency()));
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
