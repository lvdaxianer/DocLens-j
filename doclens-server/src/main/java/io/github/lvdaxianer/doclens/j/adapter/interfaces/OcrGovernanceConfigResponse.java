package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrGovernanceConfig;

/**
 * OCR 全局治理配置响应。
 *
 * @param failureThreshold 连续失败摘除阈值
 * @param probeIntervalSeconds 周期探测间隔秒数
 * @param circuitOpenSeconds 熔断打开时长秒数
 * @param recoverySuccessThreshold 恢复成功阈值
 * @param manualRecoveryAttempts 手动恢复尝试次数
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record OcrGovernanceConfigResponse(
        @JsonProperty("failure_threshold") int failureThreshold,
        @JsonProperty("probe_interval_seconds") int probeIntervalSeconds,
        @JsonProperty("circuit_open_seconds") int circuitOpenSeconds,
        @JsonProperty("recovery_success_threshold") int recoverySuccessThreshold,
        @JsonProperty("manual_recovery_attempts") int manualRecoveryAttempts
) {

    /**
     * 从领域治理配置创建响应。
     *
     * @param config 领域治理配置
     * @return 接口响应
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static OcrGovernanceConfigResponse from(OcrGovernanceConfig config) {
        return new OcrGovernanceConfigResponse(config.failureThreshold(), config.probeIntervalSeconds(),
                config.circuitOpenSeconds(), config.recoverySuccessThreshold(), config.manualRecoveryAttempts());
    }
}
