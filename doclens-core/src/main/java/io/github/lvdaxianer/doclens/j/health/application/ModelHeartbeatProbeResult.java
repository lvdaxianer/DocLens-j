package io.github.lvdaxianer.doclens.j.health.application;

import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthFailureType;

/**
 * 模型心跳探测结果。
 *
 * @param isSuccessful 是否成功
 * @param failureType 失败类型
 * @param message 失败摘要
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record ModelHeartbeatProbeResult(
        boolean isSuccessful,
        ModelHealthFailureType failureType,
        String message
) {

    private static final String EMPTY_MESSAGE = "";

    /**
     * 创建模型心跳探测结果。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public ModelHeartbeatProbeResult {
        failureType = failureType == null ? ModelHealthFailureType.UNKNOWN : failureType;
        message = message == null ? EMPTY_MESSAGE : message.trim();
    }

    /**
     * 创建成功探测结果。
     *
     * @return 成功探测结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static ModelHeartbeatProbeResult success() {
        return new ModelHeartbeatProbeResult(true, ModelHealthFailureType.UNKNOWN, EMPTY_MESSAGE);
    }

    /**
     * 创建失败探测结果。
     *
     * @param failureType 失败类型
     * @param message 失败摘要
     * @return 失败探测结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static ModelHeartbeatProbeResult failure(ModelHealthFailureType failureType, String message) {
        return new ModelHeartbeatProbeResult(false, failureType, message);
    }
}
