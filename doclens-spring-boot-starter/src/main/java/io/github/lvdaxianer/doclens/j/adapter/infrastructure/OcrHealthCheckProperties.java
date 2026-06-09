package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrHealthGovernance;

/**
 * OCR 健康检查阈值配置。
 *
 * @param healthFailureThreshold 健康检查失败摘除阈值
 * @param recoverySuccessThreshold 恢复成功阈值
 * @param circuitOpenSeconds 熔断打开持续秒数
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record OcrHealthCheckProperties(
        int healthFailureThreshold,
        int recoverySuccessThreshold,
        int circuitOpenSeconds
) {

    /**
     * 创建兼容旧调用方式的健康检查阈值配置。
     *
     * @param healthFailureThreshold 健康检查失败摘除阈值
     * @param recoverySuccessThreshold 恢复成功阈值
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrHealthCheckProperties(int healthFailureThreshold, int recoverySuccessThreshold) {
        this(healthFailureThreshold, recoverySuccessThreshold, OcrHealthGovernance.DEFAULT_CIRCUIT_OPEN_SECONDS);
    }
}
