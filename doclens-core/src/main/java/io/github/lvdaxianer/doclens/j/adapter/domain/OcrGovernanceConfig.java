package io.github.lvdaxianer.doclens.j.adapter.domain;

import java.time.OffsetDateTime;

/**
 * OCR 全局治理单例配置。
 *
 * @param id 配置主键
 * @param failureThreshold 连续失败摘除阈值
 * @param probeIntervalSeconds 周期探测间隔秒数
 * @param circuitOpenSeconds 熔断打开时长秒数
 * @param recoverySuccessThreshold 恢复成功阈值
 * @param manualRecoveryAttempts 手动恢复尝试次数
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record OcrGovernanceConfig(
        String id,
        int failureThreshold,
        int probeIntervalSeconds,
        int circuitOpenSeconds,
        int recoverySuccessThreshold,
        int manualRecoveryAttempts,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static final String SINGLETON_ID = "ocr-governance";

    /**
     * 创建默认 OCR 全局治理配置。
     *
     * @param defaults 默认治理参数
     * @param now 当前时间
     * @return 默认治理配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static OcrGovernanceConfig defaults(OcrHealthGovernance defaults, OffsetDateTime now) {
        return new OcrGovernanceConfig(SINGLETON_ID, defaults.failureThreshold(), defaults.probeIntervalSeconds(),
                defaults.circuitOpenSeconds(), defaults.recoverySuccessThreshold(),
                defaults.manualRecoveryAttempts(), now, now);
    }

    /**
     * 基于新的治理参数更新单例配置。
     *
     * @param governance 治理参数
     * @param now 当前时间
     * @return 更新后的治理配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrGovernanceConfig update(OcrHealthGovernance governance, OffsetDateTime now) {
        return new OcrGovernanceConfig(id, governance.failureThreshold(), governance.probeIntervalSeconds(),
                governance.circuitOpenSeconds(), governance.recoverySuccessThreshold(),
                governance.manualRecoveryAttempts(), createdAt, now);
    }

    /**
     * 将单例配置转换为运行时治理参数。
     *
     * @return 运行时治理参数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrHealthGovernance toGovernance() {
        return new OcrHealthGovernance(failureThreshold, probeIntervalSeconds, circuitOpenSeconds,
                recoverySuccessThreshold, manualRecoveryAttempts);
    }

    /**
     * 创建 OCR 全局治理单例配置。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrGovernanceConfig {
        id = requiredText(id, "ocr governance config id is required");
        createdAt = createdAt == null ? OffsetDateTime.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
        new OcrHealthGovernance(failureThreshold, probeIntervalSeconds, circuitOpenSeconds,
                recoverySuccessThreshold, manualRecoveryAttempts);
    }

    /**
     * 校验必填文本。
     *
     * @param value 待校验文本
     * @param message 异常消息
     * @return 去空白后的文本
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        } else {
            return value.trim();
        }
    }
}
