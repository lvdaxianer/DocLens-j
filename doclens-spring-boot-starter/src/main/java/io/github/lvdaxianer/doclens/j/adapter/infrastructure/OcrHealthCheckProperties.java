package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

/**
 * OCR 健康检查阈值配置。
 *
 * @param healthFailureThreshold 健康检查失败摘除阈值
 * @param recoverySuccessThreshold 恢复成功阈值
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record OcrHealthCheckProperties(
        int healthFailureThreshold,
        int recoverySuccessThreshold
) {
}
