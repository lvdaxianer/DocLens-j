package io.github.lvdaxianer.doclens.j.health.application;

/**
 * 单模型心跳探针。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
@FunctionalInterface
public interface ModelHeartbeatProbe {

    /**
     * 执行一次心跳探测。
     *
     * @return 心跳探测结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    ModelHeartbeatProbeResult probe();
}
