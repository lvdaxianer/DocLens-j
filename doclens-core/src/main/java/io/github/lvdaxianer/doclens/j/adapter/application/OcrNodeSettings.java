package io.github.lvdaxianer.doclens.j.adapter.application;

/**
 * OCR 节点可配置参数。
 *
 * @param endpoint 节点地址配置
 * @param scheduling 节点调度配置
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record OcrNodeSettings(
        Endpoint endpoint,
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
