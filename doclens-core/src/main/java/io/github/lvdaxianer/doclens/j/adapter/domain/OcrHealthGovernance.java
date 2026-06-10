package io.github.lvdaxianer.doclens.j.adapter.domain;

/**
 * OCR 节点健康治理策略。
 *
 * @param failureThreshold 连续失败摘除阈值
 * @param probeIntervalSeconds 周期探测间隔秒数
 * @param circuitOpenSeconds 熔断打开持续秒数
 * @param recoverySuccessThreshold 恢复成功阈值
 * @param manualRecoveryAttempts 手动恢复尝试次数
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record OcrHealthGovernance(
        int failureThreshold,
        int probeIntervalSeconds,
        int circuitOpenSeconds,
        int recoverySuccessThreshold,
        int manualRecoveryAttempts
) {
    public static final int DEFAULT_FAILURE_THRESHOLD = 3;
    public static final int DEFAULT_PROBE_INTERVAL_SECONDS = 5;
    public static final int DEFAULT_CIRCUIT_OPEN_SECONDS = 86400;
    public static final int DEFAULT_RECOVERY_SUCCESS_THRESHOLD = 3;
    public static final int DEFAULT_MANUAL_RECOVERY_ATTEMPTS = 3;

    /**
     * 创建默认 OCR 节点健康治理策略。
     *
     * @return 默认健康治理策略
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public static OcrHealthGovernance defaults() {
        return new OcrHealthGovernance(DEFAULT_FAILURE_THRESHOLD, DEFAULT_PROBE_INTERVAL_SECONDS,
                DEFAULT_CIRCUIT_OPEN_SECONDS, DEFAULT_RECOVERY_SUCCESS_THRESHOLD,
                DEFAULT_MANUAL_RECOVERY_ATTEMPTS);
    }

    /**
     * 创建 OCR 节点健康治理策略。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrHealthGovernance {
        validatePositive(failureThreshold, "ocr failure threshold must be greater than 0");
        validatePositive(probeIntervalSeconds, "ocr probe interval must be greater than 0");
        validatePositive(circuitOpenSeconds, "ocr circuit open seconds must be greater than 0");
        validatePositive(recoverySuccessThreshold, "ocr recovery success threshold must be greater than 0");
        validatePositive(manualRecoveryAttempts, "ocr manual recovery attempts must be greater than 0");
    }

    /**
     * 校验正整数配置。
     *
     * @param value 配置值
     * @param message 异常消息
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static void validatePositive(int value, String message) {
        if (value <= 0) {
            // 非正数治理参数会导致节点调度与恢复规则失效，启动时直接阻断配置错误。
            throw new IllegalArgumentException(message);
        } else {
            // 合法正整数无需额外处理。
        }
    }
}
