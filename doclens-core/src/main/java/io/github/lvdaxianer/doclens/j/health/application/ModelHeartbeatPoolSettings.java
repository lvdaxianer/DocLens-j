package io.github.lvdaxianer.doclens.j.health.application;

import java.time.OffsetDateTime;
import java.util.function.Supplier;

/**
 * 模型心跳池设置。
 *
 * @param nowSupplier 当前时间提供器
 * @param failureThreshold 失败阈值
 * @param recoverySuccessThreshold 恢复成功阈值
 * @param staleAfterSeconds 过期秒数
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record ModelHeartbeatPoolSettings(
        Supplier<OffsetDateTime> nowSupplier,
        int failureThreshold,
        int recoverySuccessThreshold,
        int staleAfterSeconds
) {

    /**
     * 创建模型心跳池设置。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public ModelHeartbeatPoolSettings {
        nowSupplier = nowSupplier == null ? OffsetDateTime::now : nowSupplier;
        failureThreshold = Math.max(1, failureThreshold);
        recoverySuccessThreshold = Math.max(1, recoverySuccessThreshold);
        staleAfterSeconds = Math.max(1, staleAfterSeconds);
    }
}
