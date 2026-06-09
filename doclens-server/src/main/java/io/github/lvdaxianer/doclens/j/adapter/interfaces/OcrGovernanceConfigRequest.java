package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrHealthGovernance;

/**
 * OCR 全局治理配置请求。
 *
 * @param failureThreshold 连续失败摘除阈值
 * @param probeIntervalSeconds 周期探测间隔秒数
 * @param circuitOpenSeconds 熔断打开时长秒数
 * @param recoverySuccessThreshold 恢复成功阈值
 * @param manualRecoveryAttempts 手动恢复尝试次数
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record OcrGovernanceConfigRequest(
        @JsonProperty("failure_threshold") int failureThreshold,
        @JsonProperty("probe_interval_seconds") int probeIntervalSeconds,
        @JsonProperty("circuit_open_seconds") int circuitOpenSeconds,
        @JsonProperty("recovery_success_threshold") int recoverySuccessThreshold,
        @JsonProperty("manual_recovery_attempts") int manualRecoveryAttempts
) {

    /**
     * 转换为 OCR 治理参数。
     *
     * @return OCR 治理参数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    OcrHealthGovernance toGovernance() {
        return new OcrHealthGovernance(failureThreshold, probeIntervalSeconds, circuitOpenSeconds,
                recoverySuccessThreshold, manualRecoveryAttempts);
    }
}
